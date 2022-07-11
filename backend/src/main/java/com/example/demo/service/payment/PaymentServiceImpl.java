package com.example.demo.service.payment;

import com.example.demo.dto.payment.PaymentRequest;
import com.example.demo.entity.payment.Payment;
import com.example.demo.entity.reservation.Reservation;
import com.example.demo.entity.reservation.ReservationStatus;
import com.example.demo.repository.payment.PaymentRepository;
import com.example.demo.repository.reservation.ReservationRepository;
import com.example.demo.utility.payment.PaymentUtility;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
@Service
public class PaymentServiceImpl implements PaymentService{

    private final PaymentRepository paymentRepository;
    private final ReservationRepository reservationRepository;
    private final PaymentUtility paymentUtility;

    @Override
    public void createPayment(PaymentRequest paymentRequest) throws JsonProcessingException {
        String accessToken = paymentUtility.createAccessToken();
        HashMap<String, String> paymentInformation = paymentUtility.getPaymentInformation(accessToken, paymentRequest);
        String id = paymentInformation.get("id");
        String amount = paymentInformation.get("amount");

        Optional<Reservation> optionalReservation = reservationRepository.findById(Long.valueOf(id));
        if (optionalReservation.isEmpty())
            throw new NoSuchElementException("Reservation ID does not exist");

        Reservation reservation = optionalReservation.get();
        if (!amount.equals(reservation.getPrice().toString()))
            throw new IllegalStateException("Amount does not match");

        log.info("Saving Payment Information");

        reservation.setStatus(ReservationStatus.RESERVED);
        Payment payment = Payment.builder()
                .price(Long.valueOf(amount))
                .build();

        payment.addReservationToPayment(reservation);
        reservationRepository.save(reservation);

    }

    @Override
    public void requestRefund(PaymentRequest paymentRequest) throws JsonProcessingException {
        String accessToken = paymentUtility.createAccessToken();
        HashMap<String, String> refundInformation = paymentUtility.requestRefund(accessToken, paymentRequest);
        String id = refundInformation.get("id");

        Optional<Reservation> optionalReservation = reservationRepository.findById(Long.valueOf(id));
        if (optionalReservation.isEmpty())
            throw new NoSuchElementException("Reservation ID does not exist");

        Reservation reservation = optionalReservation.get();

        reservation.setStatus(ReservationStatus.CANCELLED);
        reservationRepository.save(reservation);
    }
}
