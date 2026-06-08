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

    @Value("${crawling.popga-sitemap}")
    private String popgaSitemap;

    @Value("${public-api.tour.base-url}")
    private String tourApiUrl;

    @Value("${public-api.tour.service-key}")
    private String tourApiKey;

    // ───────────── 팝가 크롤링 ─────────────
    @EventListener(ApplicationReadyEvent.class)
    public void crawlPopga() {

        // DB에 팝업 데이터 있으면 스킵
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
                        if (saved >= 10) {
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
        String imageUrl = Optional.ofNullable(doc.selectFirst("img.object-contain"))
                .map(el -> el.attr("src")).orElse("");

        // 상세 설명
        String description = Optional.ofNullable(doc.selectFirst("div.whitespace-pre-line"))
                .map(Element::text).orElse("").trim();

        // 날짜 파싱
        LocalDate[] dates = parsePopgaDate(doc);
        if (dates == null) return null;

        return Spot.builder()
                .title(title)
                .description(description)
                .spotType(Spot.SpotType.POPUP)
                .area(extractArea(address))
                .address(address)
                .latitude(37.5665)
                .longitude(126.9780)
                .startDate(dates[0])
                .endDate(dates[1])
                .imageUrl(imageUrl)
                .price(0)
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

    // ───────────── 관광공사 API - 전시/행사 ─────────────
    @EventListener(ApplicationReadyEvent.class)
    public void fetchExhibitsFromPublicApi() {

        // DB에 전시 데이터 있으면 스킵
        long existingCount = spotRepository.countBySpotType(Spot.SpotType.EXHIBIT);
        if (existingCount > 0) {
            log.info("전시 데이터 {}건 이미 존재 - API 수집 스킵", existingCount);
            return;
        }

        log.info("전시/행사 API 수집 시작...");
        try {
            String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));

            String url = tourApiUrl + "/searchFestival2"
                    + "?serviceKey=" + tourApiKey
                    + "&numOfRows=10"
                    + "&pageNo=1"
                    + "&MobileOS=ETC"
                    + "&MobileApp=spotz"
                    + "&_type=json"
                    + "&arrange=C"
                    + "&eventStartDate=" + today;

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());
            JsonNode items = root.path("response").path("body").path("items").path("item");

            if (!items.isArray()) {
                log.warn("전시 API 응답 데이터 없음");
                return;
            }

            int saved = 0;
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyyMMdd");

            for (JsonNode item : items) {

                try {
                    // 1. 카테고리 코드 가져오기 (매뉴얼 기준 분류코드 확인)
                    String cat3 = item.path("cat3").asText("");

                    // 2. '전시' 관련 코드인지 검사 (A020701로 시작하는 것이 전시관/전시회)
                    // 축제/페스티벌은 보통 A020702 등으로 분류되거나 카테고리가 다릅니다.
                    if (!cat3.startsWith("A020701")) {
                        log.info("전시 데이터가 아님 (제외): {} (cat3: {})", item.path("title").asText(), cat3);
                        continue; // 축제나 공연은 무시하고 다음으로
                    }

                    String sourceId = "tour_" + item.path("contentid").asText();
                    if (spotRepository.existsBySourceId(sourceId)) continue;

                    String title = item.path("title").asText("").trim();
                    if (title.isBlank()) continue;

                    String startDateStr = item.path("eventstartdate").asText("").trim();
                    String endDateStr   = item.path("eventenddate").asText("").trim();
                    if (startDateStr.isBlank() || endDateStr.isBlank()) continue;

                    LocalDate startDate = LocalDate.parse(startDateStr, fmt);
                    LocalDate endDate   = LocalDate.parse(endDateStr, fmt);

                    String addr = item.path("addr1").asText("").trim();
                    if (addr.isBlank()) addr = "주소 미제공";

                    String mapxStr = item.path("mapx").asText("").trim();
                    String mapyStr = item.path("mapy").asText("").trim();
                    double longitude = mapxStr.isBlank() ? 126.9780 : Double.parseDouble(mapxStr);
                    double latitude  = mapyStr.isBlank() ? 37.5665  : Double.parseDouble(mapyStr);

                    // detailCommon2 로 상세 설명 가져오기
                    String contentId = item.path("contentid").asText();
                    String description = fetchDescription(contentId);

                    Spot spot = Spot.builder()
                            .title(title)
                            .description(description)
                            .spotType(Spot.SpotType.EXHIBIT)
                            .area(extractArea(addr))
                            .address(addr)
                            .latitude(latitude)
                            .longitude(longitude)
                            .startDate(startDate)
                            .endDate(endDate)
                            .imageUrl(item.path("firstimage").asText(""))
                            .price(0)
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
            log.info("전시/행사 API 수집 완료 - 신규 저장: {}건", saved);

        } catch (Exception e) {
            log.error("전시/행사 API 수집 실패", e);
        }
    }

    // detailCommon2 로 상세 설명 가져오기
    private String fetchDescription(String contentId) {
        try {
            String url = tourApiUrl + "/detailCommon2"
                    + "?serviceKey=" + tourApiKey
                    + "&MobileOS=ETC"
                    + "&MobileApp=spotz"
                    + "&_type=json"
                    + "&contentId=" + contentId
                    + "&overviewYN=Y";

            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.getBody());

            JsonNode itemNode = root.path("response").path("body").path("items").path("item");

            String overview;
            if (itemNode.isArray()) {
                overview = itemNode.path(0).path("overview").asText("");
            } else {
                overview = itemNode.path("overview").asText("");
            }

            return overview.replaceAll("<[^>]*>", "").trim();

        } catch (Exception e) {
            log.warn("상세 설명 조회 실패 [{}]: {}", contentId, e.getMessage());
            return "";
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
}