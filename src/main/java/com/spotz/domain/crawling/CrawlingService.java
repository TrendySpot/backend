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
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CrawlingService {
    private final SpotRepository spotRepository;
    private final SpotScheduleRepository scheduleRepository;
    private final RestTemplate restTemplate;

    @Value("${KAKAO_REST_API_KEY}")
    private String kakaoRestApiKey;

    @Value("${crawling.popga-sitemap}")
    private String popgaSitemap;

    @Value("${public-api.tour.base-url}")
    private String tourApiUrl;

    @Value("${public-api.tour.service-key}")
    private String tourApiKey;

    // ───────────── 내부 결과 클래스 ─────────────
    private static class CoordResult {
        final double latitude;
        final double longitude;
        final String area;

        CoordResult(double latitude, double longitude, String area) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.area = area;
        }
    }

    // ───────────── 공통: 주소 -> 좌표 + 지역 변환 ─────────────

    /**
     * 주소/장소명을 카카오 키워드 검색으로 위도·경도·지역으로 변환.
     *
     * 카카오 Local API 응답:
     *   x = 경도(longitude)
     *   y = 위도(latitude)
     *   address_name = 전체 주소 (지역 추출에 사용)
     */
    private CoordResult getCoordinates(String address) {
        if (address == null || address.isBlank() || address.equals("주소 미제공")) {
            return new CoordResult(37.5665, 126.9780, "기타");
        }

        try {
            // 층, 관, 전시실 등 세부 위치 정보 제거 → 카카오 검색 정확도 향상
            String cleanQuery = address
                    .replaceAll("(유플렉스|U-PLEX|B\\d+|\\d+층|\\d+관|\\d+전시실|\\d+홀|\\d+F|센트럴|커넥션|식품관|앞).*$", "")
                    .trim();

            if (cleanQuery.length() > 30) {
                cleanQuery = cleanQuery.substring(0, 30);
            }


            String url = UriComponentsBuilder
                    .fromHttpUrl("https://dapi.kakao.com/v2/local/search/keyword.json")
                    .queryParam("query", cleanQuery)
                    .build()
                    .toUriString();

            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoRestApiKey);
            headers.set("KA", "sdk/1.0.0 os/javascript origin/http://localhost");
            org.springframework.http.HttpEntity<String> entity =
                    new org.springframework.http.HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, org.springframework.http.HttpMethod.GET, entity, String.class);

            JsonNode root = new ObjectMapper().readTree(response.getBody());
            JsonNode documents = root.path("documents");

            if (documents.isArray() && documents.size() > 0) {
                double longitude   = documents.get(0).path("x").asDouble();
                double latitude    = documents.get(0).path("y").asDouble();
                String addressName = documents.get(0).path("address_name").asText("");
                String area        = simplifyRegion(addressName);
                log.info("변환 성공 [검색어: {}] -> 위도: {}, 경도: {}, 지역: {}", cleanQuery, latitude, longitude, area);
                return new CoordResult(latitude, longitude, area);
            } else {
                log.warn("좌표 결과 없음, 검색어: {}", cleanQuery);
            }
        } catch (Exception e) {
            log.error("지오코딩 실패: {}", e.getMessage());
        }

        return new CoordResult(37.5665, 126.9780, "기타");
    }

    // ───────────── 지역명 정제 ─────────────
    private String simplifyRegion(String addressName) {
        if (addressName.contains("서울")) return "서울";
        if (addressName.contains("경기")) return "경기";
        if (addressName.contains("부산")) return "부산";
        if (addressName.contains("대구")) return "대구";
        if (addressName.contains("인천")) return "인천";
        if (addressName.contains("광주")) return "광주";
        if (addressName.contains("대전")) return "대전";
        if (addressName.contains("울산")) return "울산";
        if (addressName.contains("제주")) return "제주";
        if (addressName.contains("강원")) return "강원";
        if (addressName.contains("충북") || addressName.contains("충청북")) return "충북";
        if (addressName.contains("충남") || addressName.contains("충청남")) return "충남";
        if (addressName.contains("전북") || addressName.contains("전라북")) return "전북";
        if (addressName.contains("전남") || addressName.contains("전라남")) return "전남";
        if (addressName.contains("경북") || addressName.contains("경상북")) return "경북";
        if (addressName.contains("경남") || addressName.contains("경상남")) return "경남";
        if (addressName.contains("세종")) return "세종";
        return "기타";
    }

    // ───────────── 팝가 크롤링 ─────────────

    @EventListener(ApplicationReadyEvent.class)
    public void crawlPopga() {
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

                        if (saved >= 2) {
                            log.info("최대 저장 개수 도달 - 크롤링 중단");
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

        // 이미지 (1순위: og:image 메타태그)
        String imageUrl = doc.select("meta[property=og:image]").attr("content");

        // 2순위: 본문 이미지
        if (imageUrl == null || imageUrl.isBlank()) {
            imageUrl = Optional.ofNullable(doc.selectFirst("img.object-cover"))
                    .map(el -> el.absUrl("src"))
                    .orElse("");
        }

        // 3순위: 기본 이미지
        if (imageUrl.isBlank() || imageUrl.equals("이미지 없음")) {
            imageUrl = "https://your-domain.com/images/default-popup.jpg";
        }

        // 상세 설명
        String description = Optional.ofNullable(doc.selectFirst("div.whitespace-pre-line"))
                .map(Element::text).orElse("").trim();

        // 날짜 파싱
        LocalDate[] dates = parsePopgaDate(doc);
        if (dates == null) return null;

        // 종료된 팝업 제외
        if (!dates[1].isAfter(LocalDate.now())) return null;

        // ✅ 좌표 + 지역 한 번에 변환
        CoordResult coords = getCoordinates(address);

        return Spot.builder()
                .title(title)
                .description(description)
                .spotType(Spot.SpotType.POPUP)
                .area(coords.area)         // ✅ 카카오 address_name 기반 지역
                .address(address)
                .latitude(coords.latitude)
                .longitude(coords.longitude)
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
            return new LocalDate[]{startDate, endDate};

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
                if (saved >= 2) break;

                try {
                    String title = item.path("title").asText("").trim();
                    if (title.isBlank()) continue;

                    // 공연/콘서트 제외
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

                    if (description.contains("자세한 정보는 '바로가기' 링크를 통해 확인하시기 바랍니다.")) {
                        log.info("무의미한 설명 문구 제외: {}", title);
                        continue;
                    }

                    // 전시회 키워드 필터
                    boolean isRealExhibit = title.contains("전시") || title.contains("특별") ||
                            title.contains("기획") || title.contains("개인전") ||
                            title.contains("박람") || title.contains("미술") ||
                            title.contains("갤러리") || title.contains("아트") ||
                            title.contains("박물관") || title.contains("사진");

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

                    if (price == 0) {
                        if (saved < 7) {
                            price = 0;
                            log.info("무료 전시회로 지정 (7개 제한): {}", title);
                        } else {
                            price = generateRandomPrice();
                            log.info("유료 전시회(랜덤 가격)로 지정: {} -> {}원", title, price);
                        }
                    }

                    String sourceId = "mcst_" + item.path("url").asText("")
                            .replaceAll(".*pSeq=", "");
                    if (spotRepository.existsBySourceId(sourceId)) continue;

                    // ✅ 좌표 + 지역 한 번에 변환 (카카오 address_name 기반 지역)
                    CoordResult coords = getCoordinates(address);

                    Spot spot = Spot.builder()
                            .title(title)
                            .description(description)
                            .spotType(Spot.SpotType.EXHIBIT)
                            .area(coords.area)         // ✅ 카카오 결과 기반 지역
                            .address(address)
                            .latitude(coords.latitude)
                            .longitude(coords.longitude)
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

    // ───────────── 공통 유틸 ─────────────

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

    /**
     * 10,000원 ~ 50,000원 사이 천 원 단위 랜덤 가격 생성
     */
    private int generateRandomPrice() {
        int randomThousand = java.util.concurrent.ThreadLocalRandom.current().nextInt(10, 51);
        return randomThousand * 1000;
    }
}