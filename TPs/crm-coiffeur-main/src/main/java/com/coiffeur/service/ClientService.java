package com.coiffeur.service;

import com.coiffeur.model.Client;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class ClientService {

    private final Map<Long, Client> store = new LinkedHashMap<>();
    private final AtomicLong counter = new AtomicLong(1);

    public ClientService() {
        // Données de démonstration
        save(Client.builder().prenom("Sophie").nom("Martin").telephone("0612345678")
                .email("sophie.martin@email.com").typeCheveux("bouclés")
                .notes("Allergie aux colorants chimiques").build());
        save(Client.builder().prenom("Lucas").nom("Dupont").telephone("0698765432")
                .email("lucas.dupont@email.com").typeCheveux("épais")
                .notes("Préfère les coupes courtes").build());
        save(Client.builder().prenom("Emma").nom("Bernard").telephone("0654321987")
                .email("emma.bernard@email.com").typeCheveux("lisses")
                .notes("Cliente fidèle depuis 3 ans").build());
        save(Client.builder().prenom("Hugo").nom("Leroy").telephone("0623456789")
                .email("hugo.leroy@email.com").typeCheveux("fins").build());
        save(Client.builder().prenom("Léa").nom("Moreau").telephone("0687654321")
                .email("lea.moreau@email.com").typeCheveux("frisés")
                .notes("Sensible du cuir chevelu").build());
    }

    public List<Client> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<Client> findById(Long id) {
        return Optional.ofNullable(store.get(id));
    }

    public List<Client> search(String query) {
        if (query == null || query.isBlank()) return findAll();
        String q = query.toLowerCase();
        return store.values().stream()
                .filter(c -> c.getNomComplet().toLowerCase().contains(q)
                        || c.getTelephone().contains(q)
                        || (c.getEmail() != null && c.getEmail().toLowerCase().contains(q)))
                .collect(Collectors.toList());
    }

    public Client save(Client client) {
        if (client.getId() == null) {
            client.setId(counter.getAndIncrement());
        }
        store.put(client.getId(), client);
        return client;
    }

    public boolean delete(Long id) {
        return store.remove(id) != null;
    }

    public int count() {
        return store.size();
    }
}
