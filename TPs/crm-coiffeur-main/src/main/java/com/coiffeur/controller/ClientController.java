package com.coiffeur.controller;

import com.coiffeur.model.Client;
import com.coiffeur.service.ClientService;
import com.coiffeur.service.RendezVousService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;
    private final RendezVousService rendezVousService;

    private static final List<String> TYPES_CHEVEUX =
            List.of("fins", "épais", "bouclés", "lisses", "frisés", "ondulés");

    // ── LISTE ─────────────────────────────────────────────────────────────────
    @GetMapping
    public String liste(@RequestParam(required = false) String search, Model model) {
        model.addAttribute("clients", clientService.search(search));
        model.addAttribute("search", search);
        return "clients/liste";
    }

    // ── DÉTAIL ────────────────────────────────────────────────────────────────
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        return clientService.findById(id).map(client -> {
            model.addAttribute("client", client);
            model.addAttribute("rendezVous", rendezVousService.findByClientId(id));
            return "clients/detail";
        }).orElse("redirect:/clients");
    }

    // ── FORMULAIRE CRÉATION ───────────────────────────────────────────────────
    @GetMapping("/nouveau")
    public String nouveauForm(Model model) {
        model.addAttribute("client", new Client());
        model.addAttribute("typesCheveux", TYPES_CHEVEUX);
        model.addAttribute("titre", "Nouveau client");
        return "clients/formulaire";
    }

    // ── FORMULAIRE ÉDITION ────────────────────────────────────────────────────
    @GetMapping("/{id}/modifier")
    public String modifierForm(@PathVariable Long id, Model model) {
        return clientService.findById(id).map(client -> {
            model.addAttribute("client", client);
            model.addAttribute("typesCheveux", TYPES_CHEVEUX);
            model.addAttribute("titre", "Modifier le client");
            return "clients/formulaire";
        }).orElse("redirect:/clients");
    }

    // ── SAUVEGARDE (création + modification) ──────────────────────────────────
    @PostMapping("/sauvegarder")
    public String sauvegarder(@Valid @ModelAttribute Client client,
                              BindingResult result,
                              Model model,
                              RedirectAttributes flash) {
        if (result.hasErrors()) {
            model.addAttribute("typesCheveux", TYPES_CHEVEUX);
            model.addAttribute("titre", client.getId() == null ? "Nouveau client" : "Modifier le client");
            return "clients/formulaire";
        }
        clientService.save(client);
        flash.addFlashAttribute("success",
                client.getId() == null
                        ? "Client créé avec succès !"
                        : "Client mis à jour avec succès !");
        return "redirect:/clients";
    }

    // ── SUPPRESSION ───────────────────────────────────────────────────────────
    @PostMapping("/{id}/supprimer")
    public String supprimer(@PathVariable Long id, RedirectAttributes flash) {
        clientService.findById(id).ifPresent(c -> {
            clientService.delete(id);
            flash.addFlashAttribute("success", "Client \"" + c.getNomComplet() + "\" supprimé.");
        });
        return "redirect:/clients";
    }
}
