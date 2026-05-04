package com.coiffeur.service;

import com.coiffeur.model.Client;
import com.coiffeur.model.RendezVous;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class RendezVousService {

    private final Map<Long, RendezVous> store = new LinkedHashMap<>();
    private final AtomicLong counter = new AtomicLong(1);
    private final ClientService clientService;

    public RendezVousService(ClientService clientService) {
        this.clientService = clientService;
        initDemoData();
    }

    private void initDemoData() {
        LocalDate today = LocalDate.now();
        save(RendezVous.builder().clientId(1L).date(today).heure(java.time.LocalTime.of(9, 0))
                .prestation("Coupe + Brushing").dureeMinutes(60).prix(45.0).statut("confirmé").build());
        save(RendezVous.builder().clientId(2L).date(today).heure(java.time.LocalTime.of(10, 30))
                .prestation("Coupe homme").dureeMinutes(30).prix(20.0).statut("confirmé").build());
        save(RendezVous.builder().clientId(3L).date(today).heure(java.time.LocalTime.of(14, 0))
                .prestation("Coloration").dureeMinutes(90).prix(75.0).statut("en attente").build());
        save(RendezVous.builder().clientId(4L).date(today.plusDays(1)).heure(java.time.LocalTime.of(11, 0))
                .prestation("Brushing").dureeMinutes(45).prix(30.0).statut("confirmé").build());
        save(RendezVous.builder().clientId(5L).date(today.plusDays(2)).heure(java.time.LocalTime.of(15, 30))
                .prestation("Coupe + Coloration").dureeMinutes(120).prix(95.0).statut("en attente").build());
        save(RendezVous.builder().clientId(1L).date(today.minusDays(3)).heure(java.time.LocalTime.of(10, 0))
                .prestation("Coupe + Brushing").dureeMinutes(60).prix(45.0).statut("terminé").build());
    }

    private void enrichir(RendezVous rv) {
        clientService.findById(rv.getClientId()).ifPresent(c -> rv.setClientNomComplet(c.getNomComplet()));
    }

    public List<RendezVous> findAll() {
        List<RendezVous> list = new ArrayList<>(store.values());
        list.forEach(this::enrichir);
        list.sort(Comparator.comparing(RendezVous::getDate).thenComparing(RendezVous::getHeure));
        return list;
    }

    public Optional<RendezVous> findById(Long id) {
        return Optional.ofNullable(store.get(id)).map(rv -> { enrichir(rv); return rv; });
    }

    public List<RendezVous> findByDate(LocalDate date) {
        return store.values().stream()
                .filter(rv -> rv.getDate().equals(date))
                .peek(this::enrichir)
                .sorted(Comparator.comparing(RendezVous::getHeure))
                .collect(Collectors.toList());
    }

    public List<RendezVous> findByClientId(Long clientId) {
        return store.values().stream()
                .filter(rv -> rv.getClientId().equals(clientId))
                .peek(this::enrichir)
                .sorted(Comparator.comparing(RendezVous::getDate).reversed())
                .collect(Collectors.toList());
    }

    public List<RendezVous> findAujourdhui() {
        return findByDate(LocalDate.now());
    }

    public RendezVous save(RendezVous rv) {
        if (rv.getId() == null) {
            rv.setId(counter.getAndIncrement());
        }
        store.put(rv.getId(), rv);
        return rv;
    }

    public boolean delete(Long id) {
        return store.remove(id) != null;
    }

    public long countByStatut(String statut) {
        return store.values().stream().filter(rv -> statut.equals(rv.getStatut())).count();
    }

    public int count() {
        return store.size();
    }
}
