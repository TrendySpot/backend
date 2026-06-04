package com.spotz.domain.crawling;

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
import org.springframework.scheduling.annotation.Scheduled;
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

    // 서버 시작시 자동 크롤링 안함
    // 개발 완료 후 주석 해제
    // @Scheduled(cron = "${crawling.schedule}")
    @Transactional
    public void crawlPopga() {
        log.info("팝가 크롤링 시작...");
        try {
            Document sitemap = Jsoup.connect(popgaSitemap).userAgent("Mozilla/5.0").timeout(10_000).get();
            Elements locs = sitemap.select("loc");
            int saved = 0;

            for (Element loc : locs) {
                String url = loc.text();
                if (!url.contains("/popup/")) continue;
                String sourceId = "popga_" + url.replaceAll(".*/popup/", "");
                if (spotRepository.existsBySourceId(sourceId)) continue;

                try {
                    Spot spot = crawlPopupDetail(url, sourceId);
                    if (spot != null) {
                        spotRepository.save(spot);
                        createSchedules(spot);
                        saved++;
                        Thread.sleep(1000);
                    }
                } catch (Exception e) {
                    log.warn("페이지 크롤링 실패: {} - {}", url, e.getMessage());
                }
            }
            log.info("팝가 크롤링 완료 - 신규 저장: {}건", saved);
        } catch (Exception e) {
            log.error("팝가 크롤링 실패", e);
        }
    }

    private Spot crawlPopupDetail(String url, String sourceId) throws Exception {
        Document doc = Jsoup.connect(url).userAgent("Mozilla/5.0").timeout(10_000).get();

        // ⚠️ 실제 팝가 HTML 구조 확인 후 selector 수정 필요
        String title = doc.selectFirst("h1.popup-title") != null
                ? doc.selectFirst("h1.popup-title").text() : null;
        if (title == null || title.isBlank()) return null;

        String address = Optional.ofNullable(doc.selectFirst(".location-address"))
                .map(Element::text).orElse("주소 미제공");
        String imageUrl = Optional.ofNullable(doc.selectFirst(".popup-thumbnail img"))
                .map(el -> el.attr("src")).orElse(null);
        String dateText = Optional.ofNullable(doc.selectFirst(".popup-date"))
                .map(Element::text).orElse("");

        LocalDate[] dates = parseDateRange(dateText);

        return Spot.builder()
                .title(title)
                .description(Optional.ofNullable(doc.selectFirst(".popup-description"))
                        .map(Element::text).orElse(""))
                .spotType(Spot.SpotType.POPUP)
                .area(extractArea(address))
                .address(address)
                .latitude(37.5665)
                .longitude(126.9780)
                .startDate(dates[0]).endDate(dates[1])
                .imageUrl(imageUrl).price(0).sourceId(sourceId)
                .build();
    }

    // 프론트 개발용 임시 @Scheduled 꺼둠
    // @Scheduled(cron = "0 0 7 * * *")
    @Transactional
    public void fetchExhibitsFromPublicApi() {
        log.info("관광공사 전시 API 수집 시작...");
        try {
            String url = tourApiUrl + "/searchFestival1"
                    + "?serviceKey=" + tourApiKey
                    + "&numOfRows=100&pageNo=1"
                    + "&MobileApp=spotz&MobileOS=ETC"
                    + "&eventStartDate=" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                    + "&listYN=Y&arrange=A&_type=json";

            restTemplate.getForObject(url, java.util.Map.class);
            log.info("전시 API 수집 완료");
        } catch (Exception e) {
            log.error("전시 API 수집 실패", e);
        }
    }

    private void createSchedules(Spot spot) {
        LocalDate cursor = spot.getStartDate();
        while (!cursor.isAfter(spot.getEndDate())) {
            scheduleRepository.save(SpotSchedule.builder()
                    .spot(spot).eventDate(cursor)
                    .totalTickets(100).remainedTickets(100).build());
            cursor = cursor.plusDays(1);
        }
    }

    private LocalDate[] parseDateRange(String text) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        try {
            String[] parts = text.split("~");
            return new LocalDate[]{ LocalDate.parse(parts[0].trim(), fmt), LocalDate.parse(parts[1].trim(), fmt) };
        } catch (Exception e) {
            return new LocalDate[]{ LocalDate.now(), LocalDate.now().plusMonths(1) };
        }
    }

    private String extractArea(String address) {
        if (address.contains("서울")) return "서울";
        if (address.contains("경기")) return "경기";
        if (address.contains("부산")) return "부산";
        if (address.contains("인천")) return "인천";
        return "기타";
    }
}
