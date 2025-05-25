package org.gyula.onlineinvoiceapi.config;

import org.apache.logging.log4j.Logger;
import org.gyula.onlineinvoiceapi.model.Apartment;
import org.gyula.onlineinvoiceapi.services.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.Scheduled;
import java.time.LocalDate;
import java.util.List;
import org.gyula.onlineinvoiceapi.model.Apartment;
import org.gyula.onlineinvoiceapi.repositories.ApartmentRepository;



@Component
public class deadlineSchedule {

    @Autowired
    private AdminService adminService;

    @Autowired
    private ApartmentRepository apartmentRepository;


    public static final Logger log = org.apache.logging.log4j.LogManager.getLogger(SecurityConfig.class);

    @Scheduled(cron = "0 0 9 * * *") // every day at 9:00 am
    public void performTask() {
        System.out.println("Scheduled task executed");
        log.info("Scheduled task executed");

        String today = String.valueOf(LocalDate.now().getDayOfMonth());
        List<Apartment> apartments = apartmentRepository.findAll();
        log.info("Found {} apartments", apartments.size());

        for (Apartment apartment : apartments) {
            if (apartment.getDeadline() != null && String.valueOf(apartment.getDeadline()).equals(today)) {
                adminService.sendReminderEmail(apartment);
                log.info("Reminder email sent for apartment: " + apartment.getId());
            }
        }

    }


}