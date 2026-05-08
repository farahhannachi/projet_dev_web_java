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
use Symfony\Component\Form\Extension\Core\Type\FileType;

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
                        'minMessage' => 'Le nom doit contenir au moins {{ limit }} caractÃ¨res',
                        'maxMessage' => 'Le nom ne peut pas dÃ©passer {{ limit }} caractÃ¨res'
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
                        'minMessage' => 'La description doit contenir au moins {{ limit }} caractÃ¨res',
                        'maxMessage' => 'La description ne peut pas dÃ©passer {{ limit }} caractÃ¨res'
                    ])
                ]
            ])
            ->add('prix', NumberType::class, [
                'label' => 'Prix (â‚¬) *',
                'scale' => 2,
                'attr' => [
                    'placeholder' => '0.00',
                    'min' => 0,
                    'max' => 9999.99,
                    'step' => 0.01
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'Le prix est obligatoire']),
                    new \Symfony\Component\Validator\Constraints\Positive(['message' => 'Le prix doit Ãªtre positif']),
                    new \Symfony\Component\Validator\Constraints\LessThanOrEqual([
                        'value' => 9999.99,
                        'message' => 'Le prix ne peut pas dÃ©passer {{ value }}â‚¬'
                    ])
                ]
            ])
            ->add('quantiteStock', NumberType::class, [
                'label' => 'QuantitÃ© en stock *',
                'attr' => [
                    'placeholder' => '0',
                    'min' => 0,
                    'max' => 99999
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'La quantitÃ© est obligatoire']),
                    new \Symfony\Component\Validator\Constraints\PositiveOrZero(['message' => 'La quantitÃ© doit Ãªtre positive ou nulle']),
                    new \Symfony\Component\Validator\Constraints\LessThanOrEqual([
                        'value' => 99999,
                        'message' => 'La quantitÃ© ne peut pas dÃ©passer {{ value }}'
                    ])
                ]
            ])
            ->add('dateExpiration', DateType::class, [
                'label' => 'Date d\'expiration *',
                'widget' => 'single_text',
                'required' => true,
                'attr' => [
                    'placeholder' => 'JJ/MM/AAAA'
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'La date d\'expiration est obligatoire']),
                    new \Symfony\Component\Validator\Constraints\GreaterThanOrEqual([
                        'value' => 'today',
                        'message' => 'La date d\'expiration ne peut pas Ãªtre antÃ©rieure Ã  aujourd\'hui'
                    ])
                ]
            ])
            ->add('categorie', TextType::class, [
                'label' => 'CatÃ©gorie *',
                'attr' => [
                    'placeholder' => 'Entrez la catÃ©gorie',
                    'minlength' => 2,
                    'maxlength' => 50
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'La catÃ©gorie est obligatoire']),
                    new \Symfony\Component\Validator\Constraints\Length([
                        'min' => 2,
                        'max' => 50,
                        'minMessage' => 'La catÃ©gorie doit contenir au moins {{ limit }} caractÃ¨res',
                        'maxMessage' => 'La catÃ©gorie ne peut pas dÃ©passer {{ limit }} caractÃ¨res'
                    ])
                ]
            ])
            ->add('image', FileType::class, [
                'label' => 'Image du produit',
                'required' => false,
                'mapped' => false,
                'attr' => [
                    'accept' => 'image/jpeg,image/jpg,image/png,image/gif,image/webp',
                    'class' => 'form-control-file'
                ],
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\File([
                        'maxSize' => '5M',
                        'mimeTypes' => [
                            'image/jpeg',
                            'image/jpg',
                            'image/png',
                            'image/gif',
                            'image/webp',
                            'image/x-png',
                            'image/pjpeg'
                        ],
                        'mimeTypesMessage' => 'Veuillez tÃ©lÃ©charger une image valide (JPEG, PNG, GIF ou WebP)',
                        'maxSizeMessage' => 'L\'image ne doit pas dÃ©passer 5Mo'
                    ])
                ]
            ])
            ->add('statut', ChoiceType::class, [
                'label' => 'Statut *',
                'required' => true,
                'choices' => [
                    'Disponible' => 'disponible',
                    'Indisponible' => 'indisponible',
                    'Rupture de stock' => 'rupture'
                ],
                'attr' => [
                    'class' => 'form-select'
                ],
                'data' => 'disponible',
                'constraints' => [
                    new \Symfony\Component\Validator\Constraints\NotBlank(['message' => 'Veuillez sÃ©lectionner un statut'])
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
