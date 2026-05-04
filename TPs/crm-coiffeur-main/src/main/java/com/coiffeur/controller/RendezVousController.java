package com.coiffeur.controller;

import com.coiffeur.model.RendezVous;
import com.coiffeur.service.ClientService;
import com.coiffeur.service.RendezVousService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/rendez-vous")
@RequiredArgsConstructor
public class RendezVousController {

    private final RendezVousService rendezVousService;
    private final ClientService clientService;

    private static final List<String> PRESTATIONS = List.of(
            "Coupe femme", "Coupe homme", "Coupe enfant",
            "Brushing", "Coupe + Brushing",
            "Coloration", "Mèches / Balayage",
            "Coupe + Coloration", "Permanente",
            "Soin / Masque", "Lissage brésilien"
    );

    private static final List<String> STATUTS = List.of(
            "en attente", "confirmé", "terminé", "annulé"
    );

    // ── LISTE ─────────────────────────────────────────────────────────────────
    @GetMapping
    public String liste(@RequestParam(required = false)
                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                        Model model) {
        if (date != null) {
            model.addAttribute("rendezVous", rendezVousService.findByDate(date));
            model.addAttribute("dateFiltre", date);
        } else {
            model.addAttribute("rendezVous", rendezVousService.findAll());
        }
        return "rendez-vous/liste";
    }

    // ── FORMULAIRE CRÉATION ───────────────────────────────────────────────────
    @GetMapping("/nouveau")
    public String nouveauForm(@RequestParam(required = false) Long clientId, Model model) {
        RendezVous rv = new RendezVous();
        rv.setStatut("en attente");
        rv.setDate(LocalDate.now());
        if (clientId != null) rv.setClientId(clientId);
        model.addAttribute("rendezVous", rv);
        model.addAttribute("clients", clientService.findAll());
        model.addAttribute("prestations", PRESTATIONS);
        model.addAttribute("statuts", STATUTS);
        model.addAttribute("titre", "Nouveau rendez-vous");
        return "rendez-vous/formulaire";
    }

    // ── FORMULAIRE ÉDITION ────────────────────────────────────────────────────
    @GetMapping("/{id}/modifier")
    public String modifierForm(@PathVariable Long id, Model model) {
        return rendezVousService.findById(id).map(rv -> {
            model.addAttribute("rendezVous", rv);
            model.addAttribute("clients", clientService.findAll());
            model.addAttribute("prestations", PRESTATIONS);
            model.addAttribute("statuts", STATUTS);
            model.addAttribute("titre", "Modifier le rendez-vous");
            return "rendez-vous/formulaire";
        }).orElse("redirect:/rendez-vous");
    }

    // ── SAUVEGARDE ────────────────────────────────────────────────────────────
    @PostMapping("/sauvegarder")
    public String sauvegarder(@Valid @ModelAttribute RendezVous rendezVous,
                              BindingResult result,
                              Model model,
                              RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("clients", clientService.findAll());
            model.addAttribute("prestations", PRESTATIONS);
            model.addAttribute("statuts", STATUTS);
            model.addAttribute("titre", rendezVous.getId() == null ? "Nouveau rendez-vous" : "Modifier le rendez-vous");
            return "rendez-vous/formulaire";
        }
        rendezVousService.save(rendezVous);
        flash.addFlashAttribute("success", "Rendez-vous enregistré avec succès !");
        return "redirect:/rendez-vous";
    }

    // ── CHANGEMENT RAPIDE DE STATUT ───────────────────────────────────────────
    @PostMapping("/{id}/statut")
    public String changerStatut(@PathVariable Long id,
                                @RequestParam String statut,
                                RedirectAttributes flash) {
        rendezVousService.findById(id).ifPresent(rv -> {
            rv.setStatut(statut);
            rendezVousService.save(rv);
            flash.addFlashAttribute("success", "Statut mis à jour : " + statut);
        });
        return "redirect:/rendez-vous";
    }

    // ── SUPPRESSION ───────────────────────────────────────────────────────────
    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Long id, RedirectAttributes flash) {
        rendezVousService.delete(id);
        flash.addFlashAttribute("success", "Rendez-vous supprimé.");
        return "redirect:/rendez-vous";
    }
}
