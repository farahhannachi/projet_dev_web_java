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
                'placeholder' => 'Sélectionner une ordonnance',
                'required' => true,
                'constraints' => [
                    new Assert\NotBlank([
                        'message' => 'Veuillez sélectionner une ordonnance'
                    ])
                ]
            ])
            ->add('produit', EntityType::class, [
                'label' => 'Produit',
                'class' => Produit::class,
                'choice_label' => function(Produit $produit) {
                    return $produit->getNom() . ' - ' . $produit->getCategorie() . ' (' . $produit->getPrix() . '€)';
                },
                'placeholder' => 'Sélectionner un produit',
                'required' => true,
                'constraints' => [
                    new Assert\NotBlank([
                        'message' => 'Veuillez sélectionner un produit'
                    ])
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
                'placeholder' => 'Sélectionner un patient',
                'required' => true,
                'constraints' => [
                    new Assert\NotBlank([
                        'message' => 'Veuillez sélectionner un patient'
                    ])
                ]
            ])
            ->add('dosage', TextType::class, [
                'label' => 'Dosage',
                'required' => true,
                'constraints' => [
                    new Assert\NotBlank([
                        'message' => 'Le dosage est obligatoire'
                    ]),
                    new Assert\Length([
                        'min' => 2,
                        'max' => 100,
                        'minMessage' => 'Le dosage doit contenir au moins {{ limit }} caractères',
                        'maxMessage' => 'Le dosage ne peut pas dépasser {{ limit }} caractères'
                    ])
                ],
                'attr' => [
                    'placeholder' => 'Ex: 500mg, 1 comprimé'
                ]
            ])
            ->add('frequence', TextType::class, [
                'label' => 'Fréquence',
                'required' => true,
                'constraints' => [
                    new Assert\NotBlank([
                        'message' => 'La fréquence est obligatoire'
                    ]),
                    new Assert\Length([
                        'min' => 2,
                        'max' => 100,
                        'minMessage' => 'La fréquence doit contenir au moins {{ limit }} caractères',
                        'maxMessage' => 'La fréquence ne peut pas dépasser {{ limit }} caractères'
                    ])
                ],
                'attr' => [
                    'placeholder' => 'Ex: 3 fois par jour, Matin et soir'
                ]
            ])
            ->add('dureeJours', IntegerType::class, [
                'label' => 'Durée (jours)',
                'required' => true,
                'constraints' => [
                    new Assert\NotBlank([
                        'message' => 'La durée est obligatoire'
                    ]),
                    new Assert\Positive([
                        'message' => 'La durée doit être un nombre positif'
                    ]),
                    new Assert\Range([
                        'min' => 1,
                        'max' => 365,
                        'notInRangeMessage' => 'La durée doit être entre {{ min }} et {{ max }} jours'
                    ])
                ],
                'attr' => [
                    'placeholder' => 'Ex: 7, 14, 30'
                ]
            ])
            ->add('dateDebut', DateType::class, [
                'label' => 'Date de début',
                'widget' => 'single_text',
                'required' => true,
                'constraints' => [
                    new Assert\NotBlank([
                        'message' => 'La date de début est obligatoire'
                    ]),
                    new Assert\Type([
                        'type' => \DateTimeInterface::class,
                        'message' => 'La date de début doit être une date valide'
                    ])
                ]
            ])
            ->add('dateFin', DateType::class, [
                'label' => 'Date de fin',
                'widget' => 'single_text',
                'required' => true,
                'constraints' => [
                    new Assert\NotBlank([
                        'message' => 'La date de fin est obligatoire'
                    ]),
                    new Assert\Type([
                        'type' => \DateTimeInterface::class,
                        'message' => 'La date de fin doit être une date valide'
                    ]),
                    new Assert\GreaterThan([
                        'propertyPath' => 'parent.all[dateDebut].data',
                        'message' => 'La date de fin doit être postérieure à la date de début'
                    ])
                ]
            ])
            ->add('notes', TextareaType::class, [
                'label' => 'Notes',
                'required' => false,
                'attr' => [
                    'placeholder' => 'Notes complémentaires (optionnel)',
                    'rows' => 4
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
