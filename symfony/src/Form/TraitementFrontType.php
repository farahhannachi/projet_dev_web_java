<?php

namespace App\Form;

use App\Entity\Traitement;
use App\Entity\Ordonnance;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints as Assert;

class TraitementFrontType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('ordonnance', EntityType::class, [
                'class' => Ordonnance::class,
                'choice_label' => function(Ordonnance $ordonnance) {
                    return $ordonnance->getNumeroOrdonnance() . ' - ' . $ordonnance->getDateOrdonnance()->format('d/m/Y');
                },
                'label' => 'Sélectionnez votre ordonnance',
                'attr' => ['class' => 'input'],
                'placeholder' => 'Choisissez une ordonnance',
                'query_builder' => function($repository) use ($options) {
                    $qb = $repository->createQueryBuilder('o');
                    if ($options['user']) {
                        $qb->where('o.utilisateur = :user')
                           ->andWhere('o.statut = :statut')
                           ->setParameter('user', $options['user'])
                           ->setParameter('statut', 'validé');
                    }
                    return $qb->orderBy('o.dateOrdonnance', 'DESC');
                },
                'constraints' => [
                    new Assert\NotBlank(['message' => 'Veuillez sélectionner une ordonnance validée'])
                ],
                'help' => 'Seules les ordonnances validées par un pharmacien sont affichées'
            ])
            ->add('dosage', TextType::class, [
                'label' => 'Dosage',
                'attr' => ['class' => 'input', 'placeholder' => 'Ex: 500mg, 2 comprimés...'],
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
                'attr' => ['class' => 'input', 'placeholder' => 'Ex: 3 fois par jour, matin et soir...'],
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La fréquence est obligatoire']),
                    new Assert\Length([
                        'max' => 255,
                        'maxMessage' => 'La fréquence ne peut pas dépasser {{ limit }} caractères'
                    ])
                ]
            ])
            ->add('dureeJours', IntegerType::class, [
                'label' => 'Durée (en jours)',
                'attr' => ['class' => 'input', 'placeholder' => 'Ex: 7, 14, 30...'],
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La durée est obligatoire']),
                    new Assert\Positive(['message' => 'La durée doit être un nombre positif']),
                    new Assert\LessThanOrEqual([
                        'value' => 365,
                        'message' => 'La durée ne peut pas dépasser 365 jours'
                    ])
                ]
            ])
            ->add('dateDebut', DateType::class, [
                'label' => 'Date de début',
                'widget' => 'single_text',
                'attr' => ['class' => 'input'],
                'data' => new \DateTime(),
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La date de début est obligatoire'])
                ]
            ])
            ->add('dateFin', DateType::class, [
                'label' => 'Date de fin',
                'widget' => 'single_text',
                'attr' => ['class' => 'input'],
                'constraints' => [
                    new Assert\NotBlank(['message' => 'La date de fin est obligatoire'])
                ]
            ])
            ->add('notes', TextareaType::class, [
                'label' => 'Notes complémentaires (Optionnel)',
                'required' => false,
                'attr' => ['class' => 'input h-24', 'placeholder' => 'Informations supplémentaires...', 'rows' => 5],
                'constraints' => [
                    new Assert\Length([
                        'max' => 5000,
                        'maxMessage' => 'Les notes ne peuvent pas dépasser {{ limit }} caractères'
                    ])
                ]
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Traitement::class,
            'user' => null,
        ]);
    }
}
