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
                'attr' => ['class' => 'input', 'placeholder' => 'Ex: ORD-2024-001']
            ])
            ->add('dateOrdonnance', DateType::class, [
                'label' => 'Date de l\'ordonnance',
                'widget' => 'single_text',
                'attr' => ['class' => 'input'],
                'data' => new \DateTime()
            ])
            ->add('dateExpiration', DateType::class, [
                'label' => 'Date d\'expiration',
                'widget' => 'single_text',
                'attr' => ['class' => 'input']
            ])
            ->add('noteMedical', TextareaType::class, [
                'label' => 'Message pour le pharmacien (Optionnel)',
                'required' => false,
                'attr' => ['class' => 'input h-24', 'placeholder' => 'Précisez un dosage, une allergie...', 'rows' => 5]
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
