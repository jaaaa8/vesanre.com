package com.vesanrebackend.service.catalog;

import com.vesanrebackend.dto.catalog.CourtAvailabilityResponse;
import com.vesanrebackend.dto.catalog.VenueAvailabilityResponse;

import java.util.ArrayList;
import java.util.List;

/** Pure merge of per-court hourly slots into venue-level free-court counts. All courts are identical (same slot list). */
public final class VenueSlots {
    private VenueSlots() {
    }

    public static List<VenueAvailabilityResponse.Slot> merge(List<List<CourtAvailabilityResponse.Slot>> perCourt) {
        if (perCourt.isEmpty()) {
            return List.of();
        }
        List<CourtAvailabilityResponse.Slot> first = perCourt.get(0); // price and grid come from the first court
        List<VenueAvailabilityResponse.Slot> out = new ArrayList<>();
        for (int i = 0; i < first.size(); i++) {
            var s = first.get(i);
            int free = 0;
            int free2h = 0;
            for (var court : perCourt) {
                if (!court.get(i).available()) {
                    continue;
                }
                free++;
                if (i + 1 < court.size() && court.get(i + 1).available()
                        && court.get(i).end().equals(court.get(i + 1).start())) {
                    free2h++;
                }
            }
            out.add(new VenueAvailabilityResponse.Slot(s.start(), s.end(), s.price(), free, free2h));
        }
        return out;
    }
}
