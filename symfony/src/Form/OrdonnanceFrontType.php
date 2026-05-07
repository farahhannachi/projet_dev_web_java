<?php

namespace App\Form;

use App\Entity\Ordonnance;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints as Assert;

class OrdonnanceFrontType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('numeroOrdonnance', TextType::class, [
                'label' => 'Numéro d\'ordonnance',
                'attr' => ['class' => 'input', 'placeholder' => 'Ex: ORD-2024-001'],
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
                'attr' => ['class' => 'input'],
                'data' => new \DateTime(),
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
                'attr' => ['class' => 'input'],
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La date d\'expiration est obligatoire']),
                    new Assert\GreaterThan([
                        'value' => 'today',
                        'message' => 'La date d\'expiration doit être dans le futur'
                    ])
                ]
            ])
            ->add('noteMedical', TextareaType::class, [
                'label' => 'Message pour le pharmacien (Optionnel)',
                'required' => false,
                'attr' => ['class' => 'input h-24', 'placeholder' => 'Précisez un dosage, une allergie...', 'rows' => 5],
                'constraints' => [
                    new Assert\Length([
                        'max' => 5000,
                        'maxMessage' => 'La note ne peut pas dépasser {{ limit }} caractères'
                    ])
                ]
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Ordonnance::class,
        ]);
    }
}
