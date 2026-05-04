package com.coiffeur.model;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Client {

    private Long id;

    @NotBlank(message = "Le prénom est obligatoire")
    @Size(min = 2, max = 50, message = "Le prénom doit contenir entre 2 et 50 caractères")
    private String prenom;

    @NotBlank(message = "Le nom est obligatoire")
    @Size(min = 2, max = 50, message = "Le nom doit contenir entre 2 et 50 caractères")
    private String nom;

    @NotBlank(message = "Le téléphone est obligatoire")
    @Pattern(regexp = "^[0-9]{10}$", message = "Le téléphone doit contenir 10 chiffres")
    private String telephone;

    @Email(message = "L'email n'est pas valide")
    private String email;

    @Size(max = 300, message = "La note ne peut pas dépasser 300 caractères")
    private String notes;

    // Type de cheveux
    private String typeCheveux; // fins, épais, bouclés, lisses, frisés

    public String getNomComplet() {
        return prenom + " " + nom;
    }
}
