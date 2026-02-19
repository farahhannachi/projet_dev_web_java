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
                'help' => 'Seules les ordonnances validées par un pharmacien sont affichées'
            ])
            ->add('dosage', TextType::class, [
                'label' => 'Dosage',
                'attr' => ['class' => 'input', 'placeholder' => 'Ex: 500mg, 2 comprimés...']
            ])
            ->add('frequence', TextType::class, [
                'label' => 'Fréquence',
                'attr' => ['class' => 'input', 'placeholder' => 'Ex: 3 fois par jour, matin et soir...']
            ])
            ->add('dureeJours', IntegerType::class, [
                'label' => 'Durée (en jours)',
                'attr' => ['class' => 'input', 'placeholder' => 'Ex: 7, 14, 30...']
            ])
            ->add('dateDebut', DateType::class, [
                'label' => 'Date de début',
                'widget' => 'single_text',
                'attr' => ['class' => 'input'],
                'data' => new \DateTime()
            ])
            ->add('dateFin', DateType::class, [
                'label' => 'Date de fin',
                'widget' => 'single_text',
                'attr' => ['class' => 'input']
            ])
            ->add('notes', TextareaType::class, [
                'label' => 'Notes complémentaires (Optionnel)',
                'required' => false,
                'attr' => ['class' => 'input h-24', 'placeholder' => 'Informations supplémentaires...', 'rows' => 5]
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
