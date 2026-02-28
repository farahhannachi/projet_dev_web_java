<?php

namespace App\Form;

use App\Entity\Coupon;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\CheckboxType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\DateTimeType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\MoneyType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints as Assert;

class CouponType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('code', TextType::class, [
                'label' => 'Code',
                'constraints' => [
                    new Assert\NotBlank(),
                    new Assert\Length(['max' => 64]),
                ],
            ])
            ->add('type', ChoiceType::class, [
                'label' => 'Type',
                'choices' => [
                    'Pourcentage' => Coupon::TYPE_PERCENTAGE,
                    'Montant fixe' => Coupon::TYPE_FIXED,
                ],
            ])
            ->add('valeur', MoneyType::class, [
                'label' => 'Valeur',
                'currency' => 'TND',
                'constraints' => [new Assert\Positive()],
            ])
            ->add('dateExpiration', DateTimeType::class, [
                'label' => 'Date expiration',
                'required' => false,
                'widget' => 'single_text',
            ])
            ->add('usageMax', IntegerType::class, [
                'label' => 'Usage max',
                'constraints' => [new Assert\Positive()],
            ])
            ->add('usageCount', IntegerType::class, [
                'label' => 'Usage actuel',
                'constraints' => [new Assert\PositiveOrZero()],
            ])
            ->add('montantMinimumPanier', MoneyType::class, [
                'label' => 'Montant minimum panier',
                'currency' => 'TND',
                'constraints' => [new Assert\PositiveOrZero()],
            ])
            ->add('actif', CheckboxType::class, [
                'label' => 'Actif',
                'required' => false,
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Coupon::class,
        ]);
    }
}

