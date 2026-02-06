<?php

namespace App\Form;

use App\Entity\Stock;
use App\Entity\Produit;
use App\Entity\Depot;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\DateTimeType;

class StockType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('seuilAlerte', NumberType::class, [
                'label' => 'Seuil d\'alerte *',
                'attr' => [
                    'placeholder' => '0',
                    'min' => 0,
                    'max' => 99999
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'Le seuil d\'alerte est obligatoire']),
                    new \Symfony\Component\Validator\Constraints\PositiveOrZero(['message' => 'Le seuil d\'alerte doit être positif ou nul']),
                    new \Symfony\Component\Validator\Constraints\LessThanOrEqual([
                        'value' => 99999,
                        'message' => 'Le seuil d\'alerte ne peut pas dépasser {{ value }}'
                    ])
                ]
            ])
            ->add('dateExpiration', DateType::class, [
                'label' => 'Date d\'expiration',
                'widget' => 'single_text',
                'required' => false,
                'empty_data' => null,
                'attr' => [
                    'placeholder' => 'JJ/MM/AAAA'
                ]
            ])
            ->add('etatStock', ChoiceType::class, [
                'label' => 'État du stock *',
                'choices' => [
                    'Disponible' => 'disponible',
                    'En rupture' => 'rupture',
                    'En alerte' => 'alerte',
                    'En commande' => 'commande'
                ],
                'attr' => [
                    'placeholder' => 'Choisir un état'
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'Veuillez sélectionner un état'])
                ]
            ])
            ->add('dateDerniereMiseAJour', DateTimeType::class, [
                'label' => 'Date de dernière mise à jour',
                'widget' => 'single_text',
                'required' => false,
                'attr' => [
                    'placeholder' => 'JJ/MM/AAAA HH:MM'
                ]
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Stock::class,
        ]);
    }
}
