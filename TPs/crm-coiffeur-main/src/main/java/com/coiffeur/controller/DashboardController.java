package com.coiffeur.controller;

import com.coiffeur.service.ClientService;
import com.coiffeur.service.RendezVousService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final ClientService clientService;
    private final RendezVousService rendezVousService;

    @GetMapping("/")
    public String dashboard(Model model) {
        model.addAttribute("totalClients",    clientService.count());
        model.addAttribute("totalRdv",        rendezVousService.count());
        model.addAttribute("rdvAujourdhui",   rendezVousService.findAujourdhui());
        model.addAttribute("rdvConfirmes",    rendezVousService.countByStatut("confirmé"));
        model.addAttribute("rdvEnAttente",    rendezVousService.countByStatut("en attente"));
        model.addAttribute("rdvTermines",     rendezVousService.countByStatut("terminé"));
        model.addAttribute("derniersClients", clientService.findAll().stream().limit(5).toList());
        return "dashboard";
    }
}
