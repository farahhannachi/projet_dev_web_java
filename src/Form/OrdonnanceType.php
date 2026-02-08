<?php

namespace App\Form;

use App\Entity\Ordonnance;
use App\Entity\Utilisateur;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints as Assert;

class OrdonnanceType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('numeroOrdonnance', TextType::class, [
                'label' => 'Numéro d\'ordonnance',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Le numéro d\'ordonnance est obligatoire']),
                    new Assert\Length([
                        'max' => 100,
                        'maxMessage' => 'Le numéro ne peut pas dépasser {{ limit }} caractères'
                    ])
                ]
            ])
            ->add('dateOrdonnance', DateType::class, [
                'label' => 'Date de l\'ordonnance',
                'widget' => 'single_text',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La date de l\'ordonnance est obligatoire']),
                    new Assert\LessThanOrEqual([
                        'value' => 'today',
                        'message' => 'La date ne peut pas être dans le futur'
                    ])
                ]
            ])
            ->add('dateExpiration', DateType::class, [
                'label' => 'Date d\'expiration',
                'widget' => 'single_text',
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La date d\'expiration est obligatoire']),
                    new Assert\GreaterThan([
                        'value' => 'today',
                        'message' => 'La date d\'expiration doit être dans le futur'
                    ])
                ]
            ])
            ->add('noteMedical', TextareaType::class, [
                'label' => 'Note médicale',
                'required' => false,
                'constraints' => [
                    new Assert\Length([
                        'max' => 5000,
                        'maxMessage' => 'La note ne peut pas dépasser {{ limit }} caractères'
                    ])
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
        ;
        
        // Ajouter le champ statut seulement en mode édition
        if ($options['is_edit']) {
            $builder->add('statut', ChoiceType::class, [
                'label' => 'Statut',
                'choices' => [
                    'En attente' => 'en attente',
                    'Validé' => 'validé',
                    'Rejeté' => 'rejeté'
                ],
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Le statut est obligatoire']),
                    new Assert\Choice([
                        'choices' => ['en attente', 'validé', 'rejeté'],
                        'message' => 'Statut invalide'
                    ])
                ]
            ]);
        }
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Ordonnance::class,
            'is_edit' => false,
        ]);
    }
}
