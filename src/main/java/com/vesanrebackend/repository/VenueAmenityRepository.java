package com.vesanrebackend.repository;

import com.vesanrebackend.entity.venue.VenueAmenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface VenueAmenityRepository extends JpaRepository<VenueAmenity, VenueAmenity.VenueAmenityId> {
    @Query("select va from VenueAmenity va join fetch va.amenity where va.venue.id = :venueId order by va.amenity.name")
    List<VenueAmenity> findByVenueId(@Param("venueId") UUID venueId);

    // Bulk delete executes immediately, so re-inserting the same keys afterwards is safe.
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from VenueAmenity va where va.venue.id = :venueId")
    void deleteByVenueId(@Param("venueId") UUID venueId);
}
