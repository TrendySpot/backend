package com.spotz.domain.crawling;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotz.domain.spot.Spot;
import com.spotz.domain.spot.SpotRepository;
import com.spotz.domain.spot.SpotSchedule;
import com.spotz.domain.spot.SpotScheduleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlingService {
    private final SpotRepository spotRepository;
    private final SpotScheduleRepository scheduleRepository;
    private final RestTemplate restTemplate;

    // CrawlingService.java 상단에 추가
    @Value("${KAKAO_REST_API_KEY}")
    private String kakaoRestApiKey;

    // 1. 주소 -> 좌표 변환 메서드 추가
    private double[] getCoordinates(String address) {
        if (address == null || address.equals("주소 미제공")) return new double[]{37.5665, 126.9780};

        try {
            // 1. 핵심: "현대백화점 신촌점"이나 "신촌로 83"만 남기도록 정제
            // (U-PLEX, B2, 층, 상세정보 제거)
            String cleanQuery = address.replaceAll("(U-PLEX|B\\d+|\\d+층|센트럴|커넥션|식품관|앞).*$", "").trim();

            // 2. 혹시나 길면 25자까지만 (한글 25자는 인코딩해도 75~80바이트 내외라 안전함)
            if (cleanQuery.length() > 25) {
                cleanQuery = cleanQuery.substring(0, 25);
            }

            // 2. UriComponentsBuilder를 사용해 안전하게 URL 생성
            // 이 방식은 RestTemplate이 인코딩을 두 번 하는 문제를 방지합니다.
            String url = UriComponentsBuilder.fromHttpUrl("https://dapi.kakao.com/v2/local/search/keyword.json")
                    .queryParam("query", cleanQuery)
                    .build()
                    .toUriString();

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);

            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class);

            JsonNode root = new ObjectMapper().readTree(response.getBody());
            JsonNode documents = root.path("documents");

            if (documents.isArray() && documents.size() > 0) {
                double x = documents.get(0).path("x").asDouble();
                double y = documents.get(0).path("y").asDouble();
                log.info("성공! [검색어: {}] -> 위도: {}, 경도: {}", cleanQuery, y, x);
                return new double[]{y, x};
            } else {
                log.warn("결과 없음, 검색어: {}", cleanQuery);
            }
        } catch (Exception e) {
            log.error("지오코딩 실패: {}", e.getMessage());
        }
        return new double[]{37.5665, 126.9780};
    }

    @Value("${crawling.popga-sitemap}")
    private String popgaSitemap;

    @Value("${public-api.tour.base-url}")
    private String tourApiUrl;

    @Value("${public-api.tour.service-key}")
    private String tourApiKey;

    // ───────────── 팝가 크롤링 ─────────────
    @EventListener(ApplicationReadyEvent.class)
    public void crawlPopga() {

       //  DB에 팝업 데이터 있으면 스킵
        long existingCount = spotRepository.countBySpotType(Spot.SpotType.POPUP);
        if (existingCount > 0) {
            log.info("팝업 데이터 {}건 이미 존재 - 크롤링 스킵", existingCount);
            return;
        }

        log.info("팝가 크롤링 시작...");
        try {
            Document mainSitemap = Jsoup.connect(popgaSitemap)
                    .userAgent("Mozilla/5.0")
                    .timeout(10_000)
                    .get();

            Elements subSitemaps = mainSitemap.select("loc");
            int saved = 0;

            for (Element subSitemap : subSitemaps) {
                String subUrl = subSitemap.text();

                try {
                    Document sub = Jsoup.connect(subUrl)
                            .userAgent("Mozilla/5.0")
                            .timeout(10_000)
                            .get();

                    Elements locs = sub.select("loc");

                    for (Element loc : locs) {
                        String url = loc.text();

                        if (!url.contains("/popup/")) continue;

                        // 최대 10개 제한
                        if (saved >= 2) {
                            log.info("최대 저장 개수(10개) 도달 - 크롤링 중단");
                            log.info("팝가 크롤링 완료 - 신규 저장: {}건", saved);
                            return;
                        }

                        String sourceId = "popga_" + url.replaceAll(".*/popup/", "");
                        if (spotRepository.existsBySourceId(sourceId)) continue;

                        try {
                            Spot spot = crawlPopupDetail(url, sourceId);
                            if (spot != null) {
                                saveSpot(spot);
                                saved++;
                                log.info("저장 완료: {}", spot.getTitle());
                                Thread.sleep(1000);
                            }
                        } catch (Exception e) {
                            log.warn("팝업 페이지 파싱 실패 [{}]: {}", url, e.getMessage());
                        }
                    }

                } catch (Exception e) {
                    log.warn("하위 sitemap 파싱 실패 [{}]: {}", subUrl, e.getMessage());
                }
            }

            log.info("팝가 크롤링 완료 - 신규 저장: {}건", saved);

        } catch (Exception e) {
            log.error("팝가 크롤링 실패", e);
        }
    }

    private Spot crawlPopupDetail(String url, String sourceId) throws Exception {
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(10_000)
                .get();

        // 제목
        String title = Optional.ofNullable(doc.selectFirst("h1.text-2xl"))
                .map(Element::text).orElse("").trim();
        if (title.isBlank()) return null;

        // 주소
        String address = Optional.ofNullable(doc.selectFirst("p.wrap-break-word"))
                .map(Element::text).orElse("주소 미제공").trim();

        // 이미지
        String imageUrl = doc.select("meta[property=og:image]").attr("content");

// 2순위: 만약 메타 태그가 비어있다면, 차선책으로 본문 이미지 태그를 찾되 절대 경로(absUrl)로 안전하게 가져옵니다.
        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = Optional.ofNullable(doc.selectFirst("img.object-cover")) // 보통 상세 포스터는 cover나 contain을 씀
                    .map(el -> el.absUrl("src")) // 👈 absUrl을 써야 주소가 http://부터 끝까지 안전하게 다 붙습니다.
                    .orElse("");
        }

// 3순위: 이것마저 없다면 리액트 화면 깨짐 방지용 더미(디폴트) 이미지 주소를 넣어둡니다.
        if (imageUrl.isBlank() || imageUrl.equals("이미지 없음")) {
            imageUrl = "https://your-domain.com/images/default-popup.jpg"; // 👈 준비하신 기본 이미지 주소 입력
        }

        // 상세 설명
        String description = Optional.ofNullable(doc.selectFirst("div.whitespace-pre-line"))
                .map(Element::text).orElse("").trim();

        // 날짜 파싱
        LocalDate[] dates = parsePopgaDate(doc);
        if (dates == null) return null;

        // 종료된 팝업 제외
        if (!dates[1].isAfter(LocalDate.now())) {
            return null;
        }

        double[] coords = getCoordinates(address);

        return Spot.builder()
                .title(title)
                .description(description)
                .spotType(Spot.SpotType.POPUP)
                .area(extractArea(address))
                .address(address)
                .latitude(coords[0])  // ⭕ 변환된 위도
                .longitude(coords[1]) // ⭕ 변환된 경도
                .startDate(dates[0])
                .endDate(dates[1])
                .imageUrl(imageUrl)
                .price(generateRandomPrice())
                .sourceId(sourceId)
                .build();
    }

    private LocalDate[] parsePopgaDate(Document doc) {
        try {
            Element dateEl = doc.selectFirst("p.text-right.text-slate-800");
            if (dateEl == null) return null;

            String dateText = dateEl.text()
                    .replaceAll("\\(.*?\\)", "")
                    .replaceAll("\\s+", " ")
                    .trim();

            String[] parts = dateText.split("~");
            if (parts.length < 2) return null;

            LocalDate startDate = parseShortDate(parts[0].trim());
            LocalDate endDate   = parseShortDate(parts[1].trim());

            if (startDate == null || endDate == null) return null;
            return new LocalDate[]{ startDate, endDate };

        } catch (Exception e) {
            log.warn("날짜 파싱 실패: {}", e.getMessage());
            return null;
        }
    }

    private LocalDate parseShortDate(String dateStr) {
        try {
            String cleaned = dateStr.replaceAll("\\s", "")
                    .replaceAll("\\.", "-")
                    .replaceAll("-$", "");
            String[] parts = cleaned.split("-");
            if (parts.length < 3) return null;

            String year  = "20" + parts[0];
            String month = parts[1];
            String day   = parts[2];

            return LocalDate.parse(year + "-" + month + "-" + day);
        } catch (Exception e) {
            return null;
        }
    }

    // 1. 핵심: 건물명(키워드)을 던져서 지역명(서울/경기 등)을 받아오는 메서드
    private String fetchAreaByKeyword(String placeName) {
        if (placeName == null || placeName.isBlank()) return "기타";

        try {
            String url = UriComponentsBuilder.fromHttpUrl("https://dapi.kakao.com/v2/local/search/keyword.json")
                    .queryParam("query", placeName)
                    .build().toUriString();

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, org.springframework.http.HttpMethod.GET, entity, String.class);
            JsonNode root = new ObjectMapper().readTree(response.getBody());
            JsonNode documents = root.path("documents");

            if (documents.isArray() && documents.size() > 0) {
                // 주소 필드(address_name)를 바로 활용하여 정제
                return simplifyRegion(documents.get(0).path("address_name").asText());
            }
        } catch (Exception e) {
            log.error("지역 추출 실패 [{}]: {}", placeName, e.getMessage());
        }
        return "기타";
    }

    // 2. 정제용 (이건 있어야 합니다)
    private String simplifyRegion(String region) {
        if (region.contains("서울")) return "서울";
        if (region.contains("경기")) return "경기";
        if (region.contains("부산")) return "부산";
        if (region.contains("인천")) return "인천";
        if (region.contains("대구")) return "대구";
        if (region.contains("광주")) return "광주";
        if (region.contains("대전")) return "대전";
        if (region.contains("울산")) return "울산";
        if (region.contains("제주")) return "제주";
        return "기타";
    }


    // ───────────── 관광공사 API - 전시/행사 ─────────────

    @EventListener(ApplicationReadyEvent.class)
    public void fetchExhibitsFromPublicApi() {

        long existingCount = spotRepository.countBySpotType(Spot.SpotType.EXHIBIT);
        if (existingCount > 0) {
            log.info("전시 데이터 {}건 이미 존재 - API 수집 스킵", existingCount);
            return;
        }

        log.info("전시 API 수집 시작...");
        try {
            String url = tourApiUrl + "?serviceKey=" + tourApiKey
                    + "&numOfRows=300&pageNo=1";

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode items = root.path("response").path("body")
                    .path("items").path("item");

            if (!items.isArray()) {
                log.warn("전시 API 응답 데이터 없음");
                return;
            }

            int saved = 0;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");

            for (JsonNode item : items) {
                //최대 10개 저장
                if (saved >= 2) break;

                try {
                    String title = item.path("title").asText("").trim();
                    if (title.isBlank()) continue;

                    // 공연/콘서트 제외 (전시회만)
                    if (title.contains("콘서트") || title.contains("공연") ||
                            title.contains("오케스트라") || title.contains("연주회") ||
                            title.contains("뮤지컬") || title.contains("축제") ||
                            title.contains("페스티벌")) {
                        log.info("공연/축제 제외: {}", title);
                        continue;
                    }

                    // eventPeriod 파싱 ("20260529 ~ 20261031")
                    String eventPeriod = item.path("eventPeriod").asText("").trim();
                    if (eventPeriod.isBlank() || !eventPeriod.contains("~")) continue;

                    String[] dates = eventPeriod.split("~");
                    if (dates.length < 2) continue;

                    LocalDate startDate = LocalDate.parse(dates[0].trim(), fmt);
                    LocalDate endDate   = LocalDate.parse(dates[1].trim(), fmt);

                    // 오늘보다 종료일이 이전이거나 오늘이면 제외
                    if (!endDate.isAfter(LocalDate.now())) {
                        log.info("종료된 전시 제외: {} ({})", title, endDate);
                        continue;
                    }
                    // 주소
                    String address = item.path("eventSite").asText("").trim();
                    if (address.isBlank()) address = "주소 미제공";

                    // 설명 (HTML 태그 제거)
                    String description = item.path("description").asText("")
                            .replaceAll("<[^>]*>", "").trim();

                    // "바로가기" 안내용 무의미한 설명 데이터 제외 필터링
                    if (description.contains("자세한 정보는 '바로가기' 링크를 통해 확인하시기 바랍니다.")) {
                        log.info("무의미한 설명 문구 제외: {}", title);
                        continue;
                    }

                    // 1. 전시회 관련 핵심 단어가 제목에 포함되어 있는지 확인
                    boolean isRealExhibit = title.contains("전시") || title.contains("특별") ||
                            title.contains("기획") || title.contains("개인전") ||
                            title.contains("박람") || title.contains("미술") ||
                            title.contains("갤러리") || title.contains("아트") ||
                            title.contains("박물관") || title.contains("사진");

                    // 2. 만약 위 단어가 하나도 안 들어있다면 전시회가 아니라고 판단하고 쳐내기
                    if (!isRealExhibit) {
                        log.info("전시회 아님 (제목 미달로 제외): {}", title);
                        continue;
                    }

                    // 가격
                    String chargeRaw = item.path("charge").asText("").trim();
                    int price = 0;
                    if (!chargeRaw.isBlank() && !chargeRaw.equals("null")) {
                        String digits = chargeRaw.replaceAll("[^0-9]", "");
                        price = digits.isBlank() ? 0 : Integer.parseInt(digits);
                    }

                    // 💡 [수정] 7개는 무조건 0원(무료), 그 외 나머지는 랜덤 가격 적용
                    if (price == 0) {
                        if (saved < 7) {
                            // 0번째부터 6번째까지 저장되는 총 7개의 전시회는 무료(0원)로 유지
                            price = 0;
                            log.info("무료 전시회로 지정 (7개 제한): {}", title);
                        } else {
                            // 7번째 저장되는 데이터부터는 랜덤 유료 가격 적용
                            price = generateRandomPrice();
                            log.info("유료 전시회(랜덤 가격)로 지정: {} -> {}원", title, price);
                        }
                    }

                    String sourceId = "mcst_" + item.path("url").asText("")
                            .replaceAll(".*pSeq=", "");
                    if (spotRepository.existsBySourceId(sourceId)) continue;


                    // 💡 [여기서부터 추가됨] 주소를 들고 카카오 지오코딩 메서드를 찔러 좌표를 알아옵니다.
                    double[] coords = getCoordinates(address);
                    double latitude = coords[0];
                    double longitude = coords[1];

                    String area = fetchAreaByKeyword(address);
                    Spot spot = Spot.builder()
                            .title(title)
                            .description(description)
                            .spotType(Spot.SpotType.EXHIBIT)
                            .area(area)
                            .address(address)
                            .latitude(latitude)    // ⭕ 계산된 진짜 위도 대입
                            .longitude(longitude)  // ⭕ 계산된 진짜 경도 대입
                            .startDate(startDate)
                            .endDate(endDate)
                            .imageUrl(item.path("imageObject").asText(""))
                            .price(price)
                            .sourceId(sourceId)
                            .build();

                    saveSpot(spot);
                    saved++;
                    log.info("전시 저장 완료: {}", title);

                } catch (Exception e) {
                    log.warn("전시 항목 저장 실패 [{}]: {}",
                            item.path("title").asText("제목없음"), e.getMessage());
                }
            }
            log.info("전시 API 수집 완료 - 신규 저장: {}건", saved);

        } catch (Exception e) {
            log.error("전시 API 수집 실패", e);
        }
    }

    // 항목별 개별 트랜잭션으로 저장
    @Transactional
    public void saveSpot(Spot spot) {
        spotRepository.save(spot);
        createSchedules(spot);
    }

    private void createSchedules(Spot spot) {
        LocalDate cursor = spot.getStartDate();
        while (!cursor.isAfter(spot.getEndDate())) {
            scheduleRepository.save(SpotSchedule.builder()
                    .spot(spot)
                    .eventDate(cursor)
                    .totalTickets(100)
                    .remainedTickets(100)
                    .build());
            cursor = cursor.plusDays(1);
        }
    }

    private String extractArea(String address) {
        if (address.contains("서울")) return "서울";
        if (address.contains("경기")) return "경기";
        if (address.contains("부산")) return "부산";
        if (address.contains("대구")) return "대구";
        if (address.contains("인천")) return "인천";
        if (address.contains("광주")) return "광주";
        if (address.contains("대전")) return "대전";
        if (address.contains("울산")) return "울산";
        if (address.contains("제주")) return "제주";
        return "기타";
    }

    // 💡 10,000원 ~ 50,000원 사이의 천 원 단위 랜덤 가격 생성
    private int generateRandomPrice() {
        // 10부터 50까지의 랜덤 정수 생성 (50을 포함하기 위해 bound를 51로 설정)
        int randomThousand = java.util.concurrent.ThreadLocalRandom.current().nextInt(10, 51);
        return randomThousand * 1000; // 1,000을 곱해 만의 자리 ~ 천의 자리 숫자로 변환
    }
}