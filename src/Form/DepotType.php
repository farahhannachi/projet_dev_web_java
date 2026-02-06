<?php

namespace App\Form;

use App\Entity\Depot;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\Form\Extension\Core\Type\DateTimeType;

class DepotType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('nomDepot', TextType::class, [
                'label' => 'Nom du dépôt *',
                'attr' => [
                    'placeholder' => 'Entrez le nom du dépôt',
                    'minlength' => 2,
                    'maxlength' => 100
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'Le nom est obligatoire']),
                    new \Symfony\Component\Validator\Constraints\Length([
                        'min' => 2,
                        'max' => 100,
                        'minMessage' => 'Le nom doit contenir au moins {{ limit }} caractères',
                        'maxMessage' => 'Le nom ne peut pas dépasser {{ limit }} caractères'
                    ])
                ]
            ])
            ->add('adresseDepot', TextType::class, [
                'label' => 'Adresse *',
                'attr' => [
                    'placeholder' => 'Entrez l\'adresse complète',
                    'minlength' => 10,
                    'maxlength' => 500
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'L\'adresse est obligatoire']),
                    new \Symfony\Component\Validator\Constraints\Length([
                        'min' => 10,
                        'max' => 500,
                        'minMessage' => 'L\'adresse doit contenir au moins {{ limit }} caractères',
                        'maxMessage' => 'L\'adresse ne peut pas dépasser {{ limit }} caractères'
                    ])
                ]
            ])
            ->add('responsableDepot', TextType::class, [
                'label' => 'Responsable',
                'required' => false,
                'attr' => [
                    'placeholder' => 'Entrez le nom du responsable',
                    'maxlength' => 255
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\Length([
                        'max' => 255,
                        'maxMessage' => 'Le nom du responsable ne peut pas dépasser {{ limit }} caractères'
                    ])
                ]
            ])
            ->add('capaciteDepot', NumberType::class, [
                'label' => 'Capacité',
                'required' => false,
                'attr' => [
                    'placeholder' => '0',
                    'min' => 0,
                    'max' => 999999
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\PositiveOrZero(['message' => 'La capacité doit être positive ou nulle']),
                    new \Symfony\Component\Validator\Constraints\LessThanOrEqual([
                        'value' => 999999,
                        'message' => 'La capacité ne peut pas dépasser {{ value }}'
                    ])
                ]
            ])
            ->add('dateCreation', DateTimeType::class, [
                'label' => 'Date de création',
                'widget' => 'single_text',
                'required' => false,
                'empty_data' => null,
                'attr' => [
                    'placeholder' => 'JJ/MM/AAAA HH:MM'
                ]
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Depot::class,
        ]);
    }
}
