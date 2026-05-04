package com.coiffeur.model;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RendezVous {

    private Long id;

    @NotNull(message = "Le client est obligatoire")
    private Long clientId;

    // Dénormalisé pour l'affichage (rempli par le service)
    private String clientNomComplet;

    @NotNull(message = "La date est obligatoire")
    private LocalDate date;

    @NotNull(message = "L'heure est obligatoire")
    private LocalTime heure;

    @NotBlank(message = "La prestation est obligatoire")
    private String prestation; // coupe, coloration, brushing, etc.

    private Integer dureeMinutes;

    @DecimalMin(value = "0.0", message = "Le prix doit être positif")
    private Double prix;

    private String statut; // confirmé, annulé, terminé, en attente

    @Size(max = 300)
    private String commentaire;

    public String getHeureFormatee() {
        return heure != null ? heure.toString() : "";
    }

    public String getStatutBadgeClass() {
        if (statut == null) return "badge-secondary";
        return switch (statut) {
            case "confirmé"   -> "badge-success";
            case "annulé"     -> "badge-danger";
            case "terminé"    -> "badge-info";
            case "en attente" -> "badge-warning";
            default           -> "badge-secondary";
        };
    }
}
