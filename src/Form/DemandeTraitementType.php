<?php

namespace App\Form;

use App\Entity\Produit;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\BirthdayType;
use Symfony\Component\Form\Extension\Core\Type\CheckboxType;
use Symfony\Component\Form\Extension\Core\Type\EmailType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints as Assert;

class DemandeTraitementType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('nom', TextType::class, [
                'label' => 'Nom & Prénom',
                'attr' => ['readonly' => true, 'class' => 'form-control'],
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Le nom et prénom sont obligatoires']),
                    new Assert\Length([
                        'min' => 2,
                        'max' => 255,
                        'minMessage' => 'Le nom doit contenir au moins {{ limit }} caractères',
                        'maxMessage' => 'Le nom ne peut pas dépasser {{ limit }} caractères'
                    ])
                ]
            ])
            ->add('email', EmailType::class, [
                'label' => 'Email',
                'attr' => ['readonly' => true, 'class' => 'form-control'],
                'constraints' => [
                    new Assert\NotBlank(['message' => 'L\'email est obligatoire']),
                    new Assert\Email(['message' => 'L\'email {{ value }} n\'est pas valide'])
                ]
            ])
            ->add('dateNaissance', BirthdayType::class, [
                'label' => 'Date de naissance',
                'widget' => 'single_text',
                'attr' => ['class' => 'form-control'],
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La date de naissance est obligatoire']),
                    new Assert\LessThan([
                        'value' => 'today',
                        'message' => 'La date de naissance doit être dans le passé'
                    ]),
                    new Assert\GreaterThan([
                        'value' => '-120 years',
                        'message' => 'La date de naissance n\'est pas valide'
                    ])
                ]
            ])
            ->add('antecedentsMedicaux', TextareaType::class, [
                'label' => 'Antécédents médicaux',
                'attr' => [
                    'class' => 'form-control',
                    'rows' => 4,
                    'placeholder' => 'Décrivez vos antécédents médicaux (allergies, maladies chroniques, traitements en cours...)'
                ],
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Les antécédents médicaux sont obligatoires']),
                    new Assert\Length([
                        'min' => 5,
                        'max' => 1000,
                        'minMessage' => 'La description doit contenir au moins {{ limit }} caractères',
                        'maxMessage' => 'La description ne peut pas dépasser {{ limit }} caractères'
                    ])
                ]
            ])
            ->add('symptomes', TextareaType::class, [
                'label' => 'Vos symptômes',
                'attr' => [
                    'class' => 'form-control',
                    'rows' => 5,
                    'placeholder' => 'Décrivez vos symptômes en détail...'
                ],
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Veuillez décrire vos symptômes']),
                    new Assert\Length([
                        'min' => 10,
                        'max' => 1000,
                        'minMessage' => 'La description doit contenir au moins {{ limit }} caractères',
                        'maxMessage' => 'La description ne peut pas dépasser {{ limit }} caractères'
                    ])
                ]
            ])
            ->add('produit', EntityType::class, [
                'label' => 'Choisir un traitement',
                'class' => Produit::class,
                'choice_label' => 'nom',
                'placeholder' => '-- Sélectionnez un produit --',
                'attr' => ['class' => 'form-control'],
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Veuillez choisir un traitement'])
                ]
            ])
            ->add('accepteConditions', CheckboxType::class, [
                'label' => 'J\'accepte les conditions générales et la politique de confidentialité',
                'mapped' => false,
                'attr' => ['class' => 'form-check-input'],
                'constraints' => [
                    new Assert\IsTrue([
                        'message' => 'Vous devez accepter les conditions générales et la politique de confidentialité'
                    ])
                ]
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([]);
    }
}
