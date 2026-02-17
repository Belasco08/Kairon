package com.kairon.repository;

import com.kairon.domain.entity.AppointmentItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentItemRepository extends JpaRepository<AppointmentItem, String> {

    void deleteByAppointmentId(String appointmentId);
    List<AppointmentItem> findByAppointmentId(String appointmentId);
}