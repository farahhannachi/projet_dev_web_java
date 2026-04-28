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
                    return $produit->getNom() . ' - ' . $produit->getCategorie() . ' (' . $produit->getPrix() . 'â‚¬)';
                },
                'placeholder' => 'SÃ©lectionner un produit',
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
                        'maxMessage' => 'Le dosage ne peut pas dÃ©passer {{ limit }} caractÃ¨res'
                    ])
                ]
            ])
            ->add('frequence', TextType::class, [
                'label' => 'FrÃ©quence',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La frÃ©quence est obligatoire']),
                    new Assert\Length([
                        'max' => 255,
                        'maxMessage' => 'La frÃ©quence ne peut pas dÃ©passer {{ limit }} caractÃ¨res'
                    ])
                ]
            ])
            ->add('dureeJours', IntegerType::class, [
                'label' => 'DurÃ©e (jours)',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La durÃ©e est obligatoire']),
                    new Assert\Positive(['message' => 'La durÃ©e doit Ãªtre positive']),
                    new Assert\Range([
                        'min' => 1,
                        'max' => 365,
                        'notInRangeMessage' => 'La durÃ©e doit Ãªtre entre {{ min }} et {{ max }} jours'
                    ])
                ]
            ])
            ->add('dateDebut', DateType::class, [
                'label' => 'Date de dÃ©but',
                'widget' => 'single_text',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La date de dÃ©but est obligatoire'])
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
                        'maxMessage' => 'Les notes ne peuvent pas dÃ©passer {{ limit }} caractÃ¨res'
                    ])
                ]
            ])
        ;
        
        // Ajouter le champ statut seulement en mode Ã©dition
        if ($isEdit) {
            $builder->add('status', ChoiceType::class, [
                'label' => 'Statut',
                'choices' => [
                    'En attente' => 'en attente',
                    'ValidÃ©' => 'validÃ©',
                    'RejetÃ©' => 'rejetÃ©',
                    'Actif' => 'actif',
                    'TerminÃ©' => 'terminÃ©',
                    'Suspendu' => 'suspendu',
                    'AnnulÃ©' => 'annulÃ©'
                ],
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Le statut est obligatoire']),
                    new Assert\Choice([
                        'choices' => ['en attente', 'validÃ©', 'rejetÃ©', 'actif', 'terminÃ©', 'suspendu', 'annulÃ©'],
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
