<?php

namespace App\Form;

use App\Entity\Commande;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\Form\Extension\Core\Type\DateTimeType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;

class CommandeType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('dateCommande', DateTimeType::class, [
                'label' => 'Date de commande *',
                'widget' => 'single_text',
                'required' => true,
                'attr' => [
                    'placeholder' => 'JJ/MM/AAAA HH:MM'
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'La date de commande est obligatoire']),
                    new \Symfony\Component\Validator\Constraints\LessThanOrEqual([
                        'value' => 'now',
                        'message' => 'La date de commande ne peut pas être dans le futur'
                    ])
                ]
            ])
            ->add('total', NumberType::class, [
                'label' => 'Total (€) *',
                'scale' => 2,
                'attr' => [
                    'placeholder' => '0.00',
                    'min' => 0.01,
                    'max' => 99999.99,
                    'step' => 0.01
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'Le total est obligatoire']),
                    new \Symfony\Component\Validator\Constraints\Positive(['message' => 'Le total doit être positif']),
                    new \Symfony\Component\Validator\Constraints\GreaterThan([
                        'value' => 0,
                        'message' => 'Le total ne peut pas être égal à 0€'
                    ]),
                    new \Symfony\Component\Validator\Constraints\LessThanOrEqual([
                        'value' => 99999.99,
                        'message' => 'Le total ne peut pas dépasser {{ value }}€'
                    ])
                ]
            ])
            ->add('modePaiement', ChoiceType::class, [
                'label' => 'Mode de paiement *',
                'choices' => [
                    'Carte bancaire' => 'carte',
                    'Espèces' => 'espece',
                    'Chèque' => 'cheque',
                    'Virement' => 'virement'
                ],
                'attr' => [
                    'placeholder' => 'Choisir un mode de paiement'
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'Veuillez sélectionner un mode de paiement'])
                ]
            ])
            ->add('statut', ChoiceType::class, [
                'label' => 'Statut *',
                'choices' => [
                    'En attente' => 'en_attente',
                    'Confirmée' => 'confirmée',
                    'En préparation' => 'préparation',
                    'Expédiée' => 'expédiée',
                    'Livrée' => 'livrée',
                    'Annulée' => 'annulée'
                ],
                'attr' => [
                    'placeholder' => 'Choisir un statut'
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'Veuillez sélectionner un statut'])
                ]
            ])
            ->add('adresseLivraison', TextType::class, [
                'label' => 'Adresse de livraison *',
                'attr' => [
                    'placeholder' => 'Entrez l\'adresse complète de livraison',
                    'minlength' => 10,
                    'maxlength' => 500
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'L\'adresse de livraison est obligatoire']),
                    new \Symfony\Component\Validator\Constraints\Length([
                        'min' => 10,
                        'max' => 500,
                        'minMessage' => 'L\'adresse doit contenir au moins {{ limit }} caractères',
                        'maxMessage' => 'L\'adresse ne peut pas dépasser {{ limit }} caractères'
                    ])
                ]
            ])
            ->add('telephone', TextType::class, [
                'label' => 'Téléphone *',
                'attr' => [
                    'placeholder' => '01234567890',
                    'maxlength' => 11
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'Le téléphone est obligatoire']),
                    new \Symfony\Component\Validator\Constraints\Length([
                        'max' => 11,
                        'maxMessage' => 'Le téléphone ne peut pas dépasser {{ limit }} caractères'
                    ])
                ]
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Commande::class,
        ]);
    }
}
