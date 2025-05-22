package org.gyula.onlineinvoiceapi.repositories;

import org.gyula.onlineinvoiceapi.model.Apartment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;


/**
 * Repository interface for managing Apartment entities.
 * Extends JpaRepository to provide CRUD operations and custom queries
 * related to apartment data, such as updating identifiers and retrieving
 * meter values.
 */
public interface ApartmentRepository extends JpaRepository<Apartment, Long> {

    @Modifying
    @Query(value = "UPDATE apartments SET id = :newId WHERE id = :currentId", nativeQuery = true)
    void updateId(@Param("currentId") Long currentId, @Param("newId") Long newId);


    @Query(value = "SELECT * FROM electricity_meter_values WHERE apartment_reference = :apartmentId ORDER BY date_of_recording DESC LIMIT 12", nativeQuery = true)
    List<Map<String, Object>> findLatestElectricityMeterValues(@Param("apartmentId") Long apartmentId);

    @Query(value = "SELECT * FROM gas_meter_values WHERE apartment_reference = :apartmentId ORDER BY date_of_recording DESC LIMIT 12", nativeQuery = true)
    List<Map<String, Object>> findLatestGasMeterValues(Long apartmentId);

    @Query(value = "SELECT * FROM water_meter_values WHERE apartment_reference = :apartmentId ORDER BY date_of_recording DESC LIMIT 12", nativeQuery = true)
    List<Map<String, Object>> findLatestWaterMeterValues(Long apartmentId);


    @Query(value = "SELECT * FROM electricity_meter_values WHERE apartment_reference = :apartmentId AND latest = true ORDER BY date_of_recording DESC LIMIT 3", nativeQuery = true)
    List<Map<String, Object>> findActiveElectricityMeterValues(@Param("apartmentId") Long apartmentId);

    @Query(value = "SELECT * FROM gas_meter_values WHERE apartment_reference = :apartmentId AND latest = true ORDER BY date_of_recording DESC LIMIT 3", nativeQuery = true)
    List<Map<String, Object>> findActiveGasMeterValues(@Param("apartmentId") Long apartmentId);

    @Query(value = "SELECT * FROM water_meter_values WHERE apartment_reference = :apartmentId AND latest = true ORDER BY date_of_recording DESC LIMIT 3", nativeQuery = true)
    List<Map<String, Object>> findActiveWaterMeterValues(@Param("apartmentId") Long apartmentId);
}

