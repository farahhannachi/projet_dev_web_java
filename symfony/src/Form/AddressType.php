<?php

namespace App\Form;

use App\Entity\Address;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;

class AddressType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('fullName', TextType::class, ['label' => 'Nom complet'])
            ->add('line1', TextType::class, ['label' => 'Adresse'])
            ->add('line2', TextType::class, ['label' => 'Complément', 'required' => false])
            ->add('city', TextType::class, ['label' => 'Ville'])
            ->add('region', TextType::class, ['label' => 'Région'])
            ->add('postalCode', TextType::class, ['label' => 'Code postal'])
            ->add('country', TextType::class, ['label' => 'Pays'])
            ->add('phone', TextType::class, ['label' => 'Téléphone', 'required' => false]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Address::class,
        ]);
    }
}

