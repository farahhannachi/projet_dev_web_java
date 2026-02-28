# Diagramme de Séquence - Mot de Passe Oublié

## Vue d'ensemble

Ce diagramme de séquence décrit le processus complet de réinitialisation du mot de passe dans l'application CURAVITA.

```mermaid
sequenceDiagram
    participant U as Utilisateur
    participant F as Form (ResetPasswordRequestType)
    participant C as ResetPasswordController
    participant R as UtilisateurRepository
    participant E as Utilisateur Entity
    participant M as EntityManager
    participant Ma as Mailer
    participant H as UserPasswordHasher

    %% ============================================
    %% PHASE 1: Demande de réinitialisation
    %% ============================================

    U->>C: POST /forgot-password (email)
    C->>F: createForm(ResetPasswordRequestType)
    F-->>C: Form instance
    C->>F: handleRequest(request)
    
    alt Form is invalid
        F-->>C: Validation errors
        C->>U: Render forgot_password.html.twig (with errors)
    else Form is valid
        C->>R: findOneBy(['email' => email])
        
        alt User not found
            R-->>C: null
            C->>U: Flash message (generic success)
            C->>U: Redirect to forgot_password
        else User found
            R-->>C: Utilisateur object
            
            %% Generate reset token
            C->>C: bin2hex(random_bytes(32))
            C->>C: new DateTime('+1 hour')
            
            %% Save token to database
            C->>E: setResetToken(token)
            C->>E: setResetTokenExpiresAt(expiresAt)
            C->>M: flush()
            M-->>E: Token saved
            
            %% Generate reset URL
            C->>C: generateUrl('app_reset_password', {token})
            
            %% Send email
            C->>Ma: send(email message)
            Ma-->>C: Email sent
            
            C->>U: Flash success message
            C->>U: Redirect to forgot_password
        end
    end

    %% ============================================
    %% PHASE 2: Réinitialisation du mot de passe
    %% ============================================

    U->>C: GET /reset-password/{token}
    C->>R: findOneBy(['resetToken' => token])
    
    alt Token invalid or not found
        R-->>C: null
        C->>U: Flash error (invalid token)
        C->>U: Redirect to forgot_password
    else Token expired
        R-->>C: Utilisateur object
        C->>E: isResetTokenValid()
        
        alt Token expired
            E-->>C: false
            C->>U: Flash error (expired)
            C->>U: Redirect to forgot_password
        else Token valid
            E-->>C: true
            
            C->>F: createForm(ResetPasswordType)
            F-->>C: Form instance
            C->>U: Render reset_password.html.twig
            
            U->>C: POST /reset-password/{token} (newPassword)
            C->>F: handleRequest(request)
            
            alt Form is invalid
                F-->>C: Validation errors
                C->>U: Render reset_password.html.twig (with errors)
            else Form is valid
                C->>H: hashPassword(user, newPassword)
                H-->>C: hashedPassword
                
                %% Update password
                C->>E: setMotDePasse(hashedPassword)
                C->>E: setResetToken(null)
                C->>E: setResetTokenExpiresAt(null)
                C->>M: flush()
                M-->>E: Password updated
                
                C->>U: Flash success message
                C->>U: Redirect to login
            end
        end
    end
```

## Détails des composants

### Contrôleurs
- **[`ResetPasswordController`](src/Controller/ResetPasswordController.php)**: Gère les deux routes du processus
  - `app_forgot_password` (ligne 23): Demande de réinitialisation
  - `app_reset_password` (ligne 89): Réinitialisation effective

### Formulaires
- **[`ResetPasswordRequestType`](src/Form/ResetPasswordRequestType.php)**: Formulaire de demande d'email
  - Champ: email (avec validation)
  
- **[`ResetPasswordType`](src/Form/ResetPasswordType.php)**: Formulaire de nouveau mot de passe
  - Champ: newPassword (RepeatedType avec confirmation)

### Entités
- **[`Utilisateur`](src/Entity/Utilisateur.php)**: Entité utilisateur
  - `resetToken` (ligne 67): Token de réinitialisation
  - `resetTokenExpiresAt` (ligne 70): Date d'expiration du token
  - `isResetTokenValid()` (ligne 317): Méthode de validation du token

## Flux détaillé

### Étape 1: Demande de réinitialisation
1. L'utilisateur saisit son adresse email
2. Le formulaire est validé (email obligatoire et format valide)
3. Le système génère un token cryptographique sécurisé (32 bytes)
4. Le token est associé à l'utilisateur avec une expiration de 1 heure
5. Un email contenant le lien de réinitialisation est envoyé
6. Un message générique est affiché (pour éviter l'énumération d'emails)

### Étape 2: Réinitialisation du mot de passe
1. L'utilisateur clique sur le lien dans l'email
2. Le système vérifie la validité du token
3. Si valide, le formulaire de nouveau mot de passe est affiché
4. L'utilisateur saisit et confirme son nouveau mot de passe
5. Le mot de passe est hashé avec bcrypt
6. L'ancien token est invalidé
7. L'utilisateur est redirigé vers la page de connexion

## Considérations de sécurité

- **Protection contre l'énumération**: Un message générique est toujours affiché
- **Expiration du token**: Le token expire après 1 heure
- **Hashage du mot de passe**: Utilisation de bcrypt via Symfony
- **Invalidation automatique**: Le token est supprimé après utilisation
