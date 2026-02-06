<?php

namespace App\Form;

use App\Entity\Produit;
use Symfony\Component\Form\AbstractType;
use Symfony\Component\Form\FormBuilderInterface;
use Symfony\Component\OptionsResolver\OptionsResolver;
use Symfony\Component\Form\Extension\Core\Type\TextType;
use Symfony\Component\Form\Extension\Core\Type\NumberType;
use Symfony\Component\Form\Extension\Core\Type\DateType;
use Symfony\Component\Form\Extension\Core\Type\ChoiceType;
use Symfony\Component\Form\Extension\Core\Type\TextareaType;
use Symfony\Component\Form\Extension\Core\Type\UrlType;

class ProduitType extends AbstractType
{
    public function buildForm(FormBuilderInterface $builder, array $options): void
    {
        $builder
            ->add('nom', TextType::class, [
                'label' => 'Nom du produit *',
                'attr' => [
                    'placeholder' => 'Entrez le nom du produit',
                    'minlength' => 2,
                    'maxlength' => 100
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'Le nom est obligatoire']),
                    new \Symfony\Component\Validator\Constraints\Length([
                        'min' => 2,
                        'max' => 100,
                        'minMessage' => 'Le nom doit contenir au moins {{ limit }} caractères',
                        'maxMessage' => 'Le nom ne peut pas dépasser {{ limit }} caractères'
                    ])
                ]
            ])
            ->add('description', TextareaType::class, [
                'label' => 'Description *',
                'attr' => [
                    'rows' => 4,
                    'placeholder' => 'Entrez la description du produit',
                    'minlength' => 10,
                    'maxlength' => 1000
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'La description est obligatoire']),
                    new \Symfony\Component\Validator\Constraints\Length([
                        'min' => 10,
                        'max' => 1000,
                        'minMessage' => 'La description doit contenir au moins {{ limit }} caractères',
                        'maxMessage' => 'La description ne peut pas dépasser {{ limit }} caractères'
                    ])
                ]
            ])
            ->add('prix', NumberType::class, [
                'label' => 'Prix (€) *',
                'scale' => 2,
                'attr' => [
                    'placeholder' => '0.00',
                    'min' => 0,
                    'max' => 9999.99,
                    'step' => 0.01
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'Le prix est obligatoire']),
                    new \Symfony\Component\Validator\Constraints\Positive(['message' => 'Le prix doit être positif']),
                    new \Symfony\Component\Validator\Constraints\LessThanOrEqual([
                        'value' => 9999.99,
                        'message' => 'Le prix ne peut pas dépasser {{ value }}€'
                    ])
                ]
            ])
            ->add('quantiteStock', NumberType::class, [
                'label' => 'Quantité en stock *',
                'attr' => [
                    'placeholder' => '0',
                    'min' => 0,
                    'max' => 99999
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'La quantité est obligatoire']),
                    new \Symfony\Component\Validator\Constraints\PositiveOrZero(['message' => 'La quantité doit être positive ou nulle']),
                    new \Symfony\Component\Validator\Constraints\LessThanOrEqual([
                        'value' => 99999,
                        'message' => 'La quantité ne peut pas dépasser {{ value }}'
                    ])
                ]
            ])
            ->add('dateExpiration', DateType::class, [
                'label' => 'Date d\'expiration',
                'widget' => 'single_text',
                'required' => false,
                'attr' => [
                    'placeholder' => 'JJ/MM/AAAA'
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\GreaterThanOrEqual([
                        'value' => 'today',
                        'message' => 'La date d\'expiration ne peut pas être antérieure à aujourd\'hui'
                    ])
                ]
            ])
            ->add('categorie', TextType::class, [
                'label' => 'Catégorie *',
                'attr' => [
                    'placeholder' => 'Entrez la catégorie',
                    'minlength' => 2,
                    'maxlength' => 50
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'La catégorie est obligatoire']),
                    new \Symfony\Component\Validator\Constraints\Length([
                        'min' => 2,
                        'max' => 50,
                        'minMessage' => 'La catégorie doit contenir au moins {{ limit }} caractères',
                        'maxMessage' => 'La catégorie ne peut pas dépasser {{ limit }} caractères'
                    ])
                ]
            ])
            ->add('image', UrlType::class, [
                'label' => 'Image (URL)',
                'required' => false,
                'attr' => [
                    'placeholder' => 'https://example.com/image.jpg',
                    'maxlength' => 500
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\Url(['message' => 'Veuillez entrer une URL valide']),
                    new \Symfony\Component\Validator\Constraints\Length([
                        'max' => 500,
                        'maxMessage' => 'L\'URL ne peut pas dépasser {{ limit }} caractères'
                    ])
                ]
            ])
            ->add('statut', ChoiceType::class, [
                'label' => 'Statut *',
                'choices' => [
                    'Disponible' => 'disponible',
                    'Indisponible' => 'indisponible',
                    'Rupture de stock' => 'rupture'
                ],
                'attr' => [
                    'placeholder' => 'Choisir un statut'
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'Veuillez sélectionner un statut'])
                ]
            ])
        ;
    }

    public function configureOptions(OptionsResolver $resolver): void
    {
        $resolver->setDefaults([
            'data_class' => Produit::class,
        ]);
    }
}
