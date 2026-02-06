<?php

namespace App\Form;

use App\Entity\Ordonnance;
use App\Entity\Traitement;
use App\Entity\Utilisateur;
use App\Entity\Produit;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints as Assert;

class TraitementType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $isEdit = $options['is_edit'] ?? true;
        
        $builder
            ->add('ordonnance', EntityType::class, [
                'label' => 'Ordonnance',
                'class' => Ordonnance::class,
                'choice_label' => function(Ordonnance $ordonnance) {
                    return $ordonnance->getNumeroOrdonnance() . ' - ' . $ordonnance->getDateOrdonnance()->format('d/m/Y');
                },
                'constraints' => [
                    new Assert\NotBlank(['message' => 'L\'ordonnance est obligatoire'])
                ]
            ])
            ->add('produit', EntityType::class, [
                'label' => 'Produit',
                'class' => Produit::class,
                'choice_label' => function(Produit $produit) {
                    return $produit->getNom() . ' - ' . $produit->getCategorie() . ' (' . $produit->getPrix() . '€)';
                },
                'placeholder' => 'Sélectionner un produit',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Le produit est obligatoire'])
                ],
                'attr' => [
                    'class' => 'form-control'
                ]
            ])
            ->add('utilisateur', EntityType::class, [
                'label' => 'Patient',
                'class' => Utilisateur::class,
                'choice_label' => function(Utilisateur $user) {
                    return $user->getNom() . ' ' . $user->getPrenom() . ' (' . $user->getEmail() . ')';
                },
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Le patient est obligatoire'])
                ]
            ])
            ->add('dosage', TextType::class, [
                'label' => 'Dosage',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Le dosage est obligatoire']),
                    new Assert\Length([
                        'max' => 255,
                        'maxMessage' => 'Le dosage ne peut pas dépasser {{ limit }} caractères'
                    ])
                ]
            ])
            ->add('frequence', TextType::class, [
                'label' => 'Fréquence',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La fréquence est obligatoire']),
                    new Assert\Length([
                        'max' => 255,
                        'maxMessage' => 'La fréquence ne peut pas dépasser {{ limit }} caractères'
                    ])
                ]
            ])
            ->add('dureeJours', IntegerType::class, [
                'label' => 'Durée (jours)',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La durée est obligatoire']),
                    new Assert\Positive(['message' => 'La durée doit être positive']),
                    new Assert\Range([
                        'min' => 1,
                        'max' => 365,
                        'notInRangeMessage' => 'La durée doit être entre {{ min }} et {{ max }} jours'
                    ])
                ]
            ])
            ->add('dateDebut', DateType::class, [
                'label' => 'Date de début',
                'widget' => 'single_text',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La date de début est obligatoire'])
                ]
            ])
            ->add('dateFin', DateType::class, [
                'label' => 'Date de fin',
                'widget' => 'single_text',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La date de fin est obligatoire'])
                ]
            ])
            ->add('notes', TextareaType::class, [
                'label' => 'Notes',
                'required' => false,
                'constraints' => [
                    new Assert\Length([
                        'max' => 5000,
                        'maxMessage' => 'Les notes ne peuvent pas dépasser {{ limit }} caractères'
                    ])
                ]
            ])
        ;
        
        // Ajouter le champ statut seulement en mode édition
        if ($isEdit) {
            $builder->add('status', ChoiceType::class, [
                'label' => 'Statut',
                'choices' => [
                    'En attente' => 'en attente',
                    'Validé' => 'validé',
                    'Rejeté' => 'rejeté',
                    'Actif' => 'actif',
                    'Terminé' => 'terminé',
                    'Suspendu' => 'suspendu',
                    'Annulé' => 'annulé'
                ],
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Le statut est obligatoire']),
                    new Assert\Choice([
                        'choices' => ['en attente', 'validé', 'rejeté', 'actif', 'terminé', 'suspendu', 'annulé'],
                        'message' => 'Statut invalide'
                    ])
                ]
            ]);
        }
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Traitement::class,
            'is_edit' => true,
        ]);
    }
}
