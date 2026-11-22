package com.vesanrebackend.service.catalog;

import com.vesanrebackend.dto.admin.PageResponse;
import com.vesanrebackend.dto.catalog.LocationResponse;
import com.vesanrebackend.dto.catalog.VenueCardResponse;
import com.vesanrebackend.service.storage.ImageStorage;
import com.vesanrebackend.util.Paging;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Public venue search and location list: plain SQL because the filters and ORDER BY vary per request. */
@Service
public class PublicVenueSearchService {
    private final NamedParameterJdbcTemplate jdbc;
    private final ImageStorage storage;

    public PublicVenueSearchService(NamedParameterJdbcTemplate jdbc, ImageStorage storage) {
        this.jdbc = jdbc;
        this.storage = storage;
    }

    public PageResponse<VenueCardResponse> search(String q, String city, String district, String sport,
                                                  BigDecimal minPrice, BigDecimal maxPrice, String sort,
                                                  int page, int size) {
        Pageable pageable = Paging.of(page, size, "id"); // only validates/clamps page and size; ORDER BY below decides order
        q = blankToNull(q);
        city = blankToNull(city);
        district = blankToNull(district);
        sport = blankToNull(sport);
        if (q != null && q.length() > 100) {
            throw bad("q must be at most 100 characters");
        }
        if ((minPrice != null && minPrice.signum() < 0) || (maxPrice != null && maxPrice.signum() < 0)) {
            throw bad("minPrice and maxPrice must be >= 0");
        }
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw bad("minPrice must not exceed maxPrice");
        }
        String order = switch (sort) {
            case "newest" -> "x.created_at desc";
            case "price_asc" -> "x.from_price asc nulls last";
            case "price_desc" -> "x.from_price desc nulls last";
            default -> throw bad("sort must be newest, price_asc or price_desc");
        };

        MapSqlParameterSource params = new MapSqlParameterSource();
        StringBuilder inner = new StringBuilder("""
                select v.id, v.name, v.address_line, v.district, v.city, v.latitude, v.longitude, v.created_at,
                       (select min(r.price_per_hour) from sporthub.court_pricing_rules r
                          join sporthub.courts c on c.id = r.court_id
                         where c.venue_id = v.id and c.status = 'ACTIVE' and r.is_active) as from_price
                  from sporthub.venues v
                 where v.status = 'ACTIVE'""");
        if (city != null) {
            inner.append(" and v.city = :city");
            params.addValue("city", city);
        }
        if (district != null) {
            inner.append(" and v.district = :district");
            params.addValue("district", district);
        }
        if (q != null) {
            inner.append(" and (v.name ilike :q escape '\\' or v.address_line ilike :q escape '\\')");
            params.addValue("q", "%" + q.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%");
        }
        if (sport != null) {
            inner.append(' ').append("""
                    and exists (select 1 from sporthub.courts c
                                   join sporthub.court_sports cs on cs.court_id = c.id
                                   join sporthub.sports s on s.id = cs.sport_id
                                  where c.venue_id = v.id and c.status = 'ACTIVE' and s.code = :sport)""");
            params.addValue("sport", sport);
        }
        StringBuilder base = new StringBuilder("select * from (").append(inner).append(") x where true");
        if (minPrice != null) {
            base.append(" and x.from_price >= :minPrice");
            params.addValue("minPrice", minPrice);
        }
        if (maxPrice != null) {
            base.append(" and x.from_price <= :maxPrice");
            params.addValue("maxPrice", maxPrice);
        }

        Long total = jdbc.queryForObject("select count(*) from (" + base + ") y", params, Long.class);
        params.addValue("size", pageable.getPageSize()).addValue("offset", pageable.getOffset());
        List<Map<String, Object>> rows = jdbc.queryForList(
                base + " order by " + order + ", x.id limit :size offset :offset", params);

        Map<UUID, String> covers = new LinkedHashMap<>();
        Map<UUID, List<String>> sports = new LinkedHashMap<>();
        Map<UUID, BigDecimal> ratingAverages = new LinkedHashMap<>();
        Map<UUID, Long> ratingCounts = new LinkedHashMap<>();
        if (!rows.isEmpty()) {
            MapSqlParameterSource ids = new MapSqlParameterSource("ids",
                    rows.stream().map(r -> (UUID) r.get("id")).toList());
            jdbc.query("select venue_id, storage_key from sporthub.venue_images where is_cover and venue_id in (:ids)",
                    ids, rs -> {
                        covers.put(rs.getObject("venue_id", UUID.class), rs.getString("storage_key"));
                    });
            jdbc.query("""
                    select distinct c.venue_id, s.name from sporthub.courts c
                      join sporthub.court_sports cs on cs.court_id = c.id
                      join sporthub.sports s on s.id = cs.sport_id
                     where c.status = 'ACTIVE' and c.venue_id in (:ids) order by s.name""", ids, rs -> {
                sports.computeIfAbsent(rs.getObject("venue_id", UUID.class), k -> new ArrayList<>())
                        .add(rs.getString("name"));
            });
            // Same rule as the venue detail: PUBLISHED reviews only, average rounded to 1 decimal.
            jdbc.query("""
                    select c.venue_id, round(avg(rv.rating), 1) as average, count(*) as total
                      from sporthub.reviews rv
                      join sporthub.bookings b on b.id = rv.booking_id
                      join sporthub.court_schedule_blocks sb on sb.id = b.schedule_block_id
                      join sporthub.courts c on c.id = sb.court_id
                     where rv.status = 'PUBLISHED' and c.venue_id in (:ids) group by c.venue_id""", ids, rs -> {
                UUID venueId = rs.getObject("venue_id", UUID.class);
                ratingAverages.put(venueId, rs.getBigDecimal("average"));
                ratingCounts.put(venueId, rs.getLong("total"));
            });
        }
        List<VenueCardResponse> cards = rows.stream().map(r -> {
            UUID id = (UUID) r.get("id");
            return new VenueCardResponse(id, (String) r.get("name"), (String) r.get("address_line"),
                    (String) r.get("district"), (String) r.get("city"), (BigDecimal) r.get("latitude"),
                    (BigDecimal) r.get("longitude"), storage.url(covers.get(id)), sports.getOrDefault(id, List.of()),
                    (BigDecimal) r.get("from_price"), ratingAverages.get(id), ratingCounts.getOrDefault(id, 0L));
        }).toList();
        return PageResponse.of(new PageImpl<>(cards, pageable, total));
    }

    public List<LocationResponse> locations() {
        Map<String, List<String>> byCity = new LinkedHashMap<>();
        jdbc.query("select distinct city, district from sporthub.venues where status = 'ACTIVE' order by city, district",
                rs -> {
                    byCity.computeIfAbsent(rs.getString("city"), k -> new ArrayList<>()).add(rs.getString("district"));
                });
        return byCity.entrySet().stream().map(e -> new LocationResponse(e.getKey(), e.getValue())).toList();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
