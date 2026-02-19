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
                'label' => 'Numéro d\'ordonnance'
            ])
            ->add('dateOrdonnance', DateType::class, [
                'label' => 'Date de l\'ordonnance',
                'widget' => 'single_text'
            ])
            ->add('dateExpiration', DateType::class, [
                'label' => 'Date d\'expiration',
                'widget' => 'single_text'
            ])
            ->add('noteMedical', TextareaType::class, [
                'label' => 'Note médicale',
                'required' => false
            ])
            ->add('utilisateur', EntityType::class, [
                'label' => 'Patient',
                'class' => Utilisateur::class,
                'choice_label' => function(Utilisateur $user) {
                    return $user->getNom() . ' ' . $user->getPrenom() . ' (' . $user->getEmail() . ')';
                }
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
