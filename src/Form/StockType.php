<?php

namespace App\Form;

use App\Entity\Stock;
use Symfony\Bridge\Doctrine\Form\Type\EntityType;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\DateTimeType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\IntegerType;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Validator\Constraints as Assert;

class StockType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('produit', EntityType::class, [
                'class' => 'App\Entity\Produit',
                'choice_label' => 'nom',
                'label' => 'Produit',
                'placeholder' => 'Sélectionner un produit',
                'constraints' => [
                    new Assert\NotBlank(message: 'Veuillez sélectionner un produit'),
                ],
                'attr' => ['class' => 'form-control'],
            ])
            ->add('depot', EntityType::class, [
                'class' => 'App\Entity\Depot',
                'choice_label' => 'nomDepot',
                'label' => 'Dépôt',
                'placeholder' => 'Sélectionner un dépôt',
                'constraints' => [
                    new Assert\NotBlank(message: 'Veuillez sélectionner un dépôt'),
                ],
                'attr' => ['class' => 'form-control'],
            ])
            ->add('quantite', IntegerType::class, [
                'label' => 'Quantité initiale',
                'constraints' => [
                    new Assert\NotBlank(message: 'La quantité est obligatoire'),
                    new Assert\Positive(message: 'La quantité doit être supérieure à 0'),
                ],
                'attr' => ['class' => 'form-control', 'min' => 1],
            ])
            ->add('seuilAlerte', IntegerType::class, [
                'label' => 'Seuil alerte',
                'constraints' => [new Assert\Positive(message: 'Le seuil d\'alerte doit être positif')],
                'attr' => ['class' => 'form-control', 'min' => 0],
                'required' => false,
            ])
            ->add('seuilCritique', IntegerType::class, [
                'label' => 'Seuil critique',
                'constraints' => [new Assert\Positive(message: 'Le seuil critique doit être positif')],
                'attr' => ['class' => 'form-control', 'min' => 0],
                'required' => false,
            ])
            ->add('dateEntree', DateType::class, [
                'label' => 'Date entrée',
                'widget' => 'single_text',
                'constraints' => [new Assert\NotBlank(message: 'La date d\'entrée est obligatoire')],
                'attr' => ['class' => 'form-control'],
            ])
            ->add('dateExpiration', DateType::class, [
                'label' => 'Date expiration',
                'widget' => 'single_text',
                'required' => true,
                'constraints' => [
                    new Assert\NotBlank(message: 'La date expiration est obligatoire pour le QR Code'),
                ],
                'attr' => ['class' => 'form-control'],
            ])
            ->add('emplacement', TextType::class, [
                'label' => 'Emplacement',
                'required' => false,
                'constraints' => [
                    new Assert\Length(max: 100, maxMessage: 'L\'emplacement ne peut pas dépasser 100 caractères'),
                ],
                'attr' => ['class' => 'form-control'],
            ])
            ->add('batchNumber', TextType::class, [
                'label' => 'Numero lot',
                'required' => true,
                'constraints' => [
                    new Assert\NotBlank(message: 'Le numero de lot est obligatoire pour le QR Code'),
                    new Assert\Length(max: 50, maxMessage: 'Le numero de lot ne peut pas dépasser 50 caractères'),
                ],
                'attr' => ['class' => 'form-control'],
            ])
            ->add('fournisseur', TextType::class, [
                'label' => 'Fournisseur',
                'required' => false,
                'constraints' => [
                    new Assert\Length(max: 100, maxMessage: 'Le fournisseur ne peut pas dépasser 100 caractères'),
                ],
                'attr' => ['class' => 'form-control'],
            ])
            ->add('notes', TextareaType::class, [
                'label' => 'Notes',
                'required' => false,
                'constraints' => [
                    new Assert\Length(max: 1000, maxMessage: 'Les notes ne peuvent pas dépasser 1000 caractères'),
                ],
                'attr' => ['class' => 'form-control', 'rows' => 3],
            ])
            ->add('etatStock', ChoiceType::class, [
                'label' => 'Etat stock',
                'choices' => [
                    'Disponible' => Stock::ETAT_DISPONIBLE,
                    'Alerte' => Stock::ETAT_ALERTE,
                    'Rupture' => Stock::ETAT_RUPTURE,
                    'Perime' => Stock::ETAT_PERIME,
                    'Expire' => Stock::ETAT_EXPIRE,
                ],
                'attr' => ['class' => 'form-control'],
            ])
            ->add('dateDerniereMiseAJour', DateTimeType::class, [
                'label' => 'Derniere mise a jour',
                'widget' => 'single_text',
                'disabled' => true,
                'data' => new \DateTime(),
                'attr' => ['class' => 'form-control', 'readonly' => true],
                'required' => false,
            ]);
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Stock::class,
            'attr' => ['novalidate' => 'novalidate'],
        ]);
    }
}

