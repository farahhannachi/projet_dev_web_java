<?php

namespace App\Form;

use App\Entity\Address;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints as Assert;

class AddressValidatedType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('fullName', TextType::class, [
                'label' => 'Nom complet',
                'constraints' => [
                    new Assert\NotBlank(message: 'Le nom complet est obligatoire.'),
                    new Assert\Length(min: 2, max: 255),
                ],
            ])
            ->add('line1', TextType::class, [
                'label' => 'Adresse',
                'constraints' => [
                    new Assert\NotBlank(message: 'L adresse est obligatoire.'),
                    new Assert\Length(min: 5, max: 255),
                ],
            ])
            ->add('line2', TextType::class, [
                'label' => 'Complement',
                'required' => false,
                'constraints' => [
                    new Assert\Length(max: 255),
                ],
            ])
            ->add('city', TextType::class, [
                'label' => 'Ville',
                'constraints' => [
                    new Assert\NotBlank(message: 'La ville est obligatoire.'),
                    new Assert\Length(min: 2, max: 120),
                ],
            ])
            ->add('region', TextType::class, [
                'label' => 'Region',
                'constraints' => [
                    new Assert\NotBlank(message: 'La region est obligatoire.'),
                    new Assert\Length(min: 2, max: 120),
                ],
            ])
            ->add('postalCode', TextType::class, [
                'label' => 'Code postal',
                'constraints' => [
                    new Assert\NotBlank(message: 'Le code postal est obligatoire.'),
                    new Assert\Length(min: 3, max: 20),
                ],
            ])
            ->add('country', TextType::class, [
                'label' => 'Pays',
                'constraints' => [
                    new Assert\NotBlank(message: 'Le pays est obligatoire.'),
                    new Assert\Length(min: 2, max: 100),
                ],
            ])
            ->add('phone', TextType::class, [
                'label' => 'Telephone',
                'required' => false,
                'constraints' => [
                    new Assert\Length(max: 20),
                ],
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Address::class,
        ]);
    }
}

