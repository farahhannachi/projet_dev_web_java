<?php

namespace App\Controller;

use App\Entity\Promotion;
use App\Entity\Produit;
use App\Entity\Utilisateur;
use App\Repository\PromotionRepository;
use App\Repository\ProduitRepository;
use Doctrine\ORM\EntityManagerInterface;
use Symfony\Bundle\FrameworkBundle\Controller\AbstractController;
use Symfony\Component\HttpFoundation\Request;
use Symfony\Component\HttpFoundation\Response;
use Symfony\Component\Routing\Annotation\Route;

class PromotionController extends AbstractController
{
    #[Route('/admin/promotions', name: 'admin_promotions')]
    public function index(PromotionRepository $promotionRepository, ProduitRepository $produitRepository, Request $request): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $search = $request->query->get('search', '');
        $sort = $request->query->get('sort', '');
        $category = $request->query->get('category', '');
        $promotionStatus = $request->query->get('promotion_status', '');
        $priceMin = $request->query->get('price_min', '');
        $priceMax = $request->query->get('price_max', '');
        
        // Get all products first for category filter
        $allProducts = $produitRepository->findBy(['statut' => 'disponible']);
        
        // Build query for products
        $queryBuilder = $produitRepository->createQueryBuilder('p')
            ->where('p.statut = :statut')
            ->setParameter('statut', 'disponible');
        
        // Apply search filter
        if (!empty($search)) {
            $queryBuilder->andWhere('p.nom LIKE :search OR p.categorie LIKE :search OR p.description LIKE :search')
                ->setParameter('search', '%' . $search . '%');
        }
        
        // Apply category filter
        if (!empty($category)) {
            $queryBuilder->andWhere('p.categorie = :category')
                ->setParameter('category', $category);
        }
        
        // Apply price range filter
        if (!empty($priceMin)) {
            $queryBuilder->andWhere('p.prix >= :priceMin')
                ->setParameter('priceMin', (float)$priceMin);
        }
        
        if (!empty($priceMax)) {
            $queryBuilder->andWhere('p.prix <= :priceMax')
                ->setParameter('priceMax', (float)$priceMax);
        }
        
        $produits = $queryBuilder->getQuery()->getResult();
        
        // Apply promotion status filter
        if (!empty($promotionStatus)) {
            $produits = array_filter($produits, function($produit) use ($promotionStatus) {
                $hasPromotion = $produit->getActivePromotion() !== null;
                return ($promotionStatus === 'with_promo' && $hasPromotion) 
                    || ($promotionStatus === 'without_promo' && !$hasPromotion);
            });
            $produits = array_values($produits);
        }
        
        // Apply price sorting if requested
        if ($sort === 'price') {
            usort($produits, function($a, $b) {
                return $a->getPrix() <=> $b->getPrix();
            });
        }
        
        return $this->render('admin/promotions_index_fixed.html.twig', [
            'produits' => $produits,
            'all_products' => $allProducts,
            'search' => $search,
            'sort' => $sort,
            'category' => $category,
            'promotion_status' => $promotionStatus,
            'price_min' => $priceMin,
            'price_max' => $priceMax
        ]);
    }

    #[Route('/admin/promotion/create/{id}', name: 'admin_promotion_create', methods: ['GET', 'POST'])]
    public function createForProduct(Produit $produit, Request $request, EntityManagerInterface $entityManager, \Symfony\Component\Validator\Validator\ValidatorInterface $validator): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        // Symfony Validator is injected
        // Check if there's already an active promotion
        $existingPromotion = $produit->getActivePromotion();

        if ($request->isMethod('POST')) {
            $titre = $request->request->get('titre');
            $description = $request->request->get('description');
            $dateDebut = $request->request->get('date_debut');
            $dateFin = $request->request->get('date_fin');
            $statut = $request->request->get('statut');
            $prixPromotionnel = $request->request->get('prix_promotionnel');

            $input = [
                'titre' => $titre,
                'description' => $description,
                'date_debut' => $dateDebut,
                'date_fin' => $dateFin,
                'statut' => $statut,
                'prix_promotionnel' => $prixPromotionnel,
            ];

            $constraints = new \Symfony\Component\Validator\Constraints\Collection([
                'titre' => [new \Symfony\Component\Validator\Constraints\NotBlank(), new \Symfony\Component\Validator\Constraints\Length(['min' => 3])],
                'description' => [new \Symfony\Component\Validator\Constraints\NotBlank()],
                'date_debut' => [new \Symfony\Component\Validator\Constraints\NotBlank(), new \Symfony\Component\Validator\Constraints\Date()],
                'date_fin' => [new \Symfony\Component\Validator\Constraints\NotBlank(), new \Symfony\Component\Validator\Constraints\Date()],
                'statut' => [new \Symfony\Component\Validator\Constraints\NotBlank()],
                'prix_promotionnel' => [new \Symfony\Component\Validator\Constraints\NotBlank(), new \Symfony\Component\Validator\Constraints\Type(['type' => 'numeric'])],
            ]);

            $violations = $validator->validate($input, $constraints);
            if (count($violations) > 0) {
                foreach ($violations as $violation) {
                    $this->addFlash('error', $violation->getMessage());
                }
                return $this->redirectToRoute('admin_promotion_create', ['id' => $produit->getId()]);
            }

            if ($existingPromotion) {
                // Update existing promotion
                $promotion = $existingPromotion;
            } else {
                // Create new promotion
                $promotion = new Promotion();
                $promotion->setProduit($produit);
                /** @var Utilisateur|null $currentUser */
                $currentUser = $this->getUser();
                $promotion->setIdAdmin($currentUser ? (int) $currentUser->getId() : 0);
            }

            /** @var string $dateDebutStr */
            $dateDebutStr = (string) $dateDebut;
            /** @var string $dateFinStr */
            $dateFinStr = (string) $dateFin;
            if (new \DateTime($dateFinStr) < new \DateTime($dateDebutStr)) {
                $this->addFlash('error', 'La date de fin doit etre superieure ou egale a la date de debut.');
                return $this->redirectToRoute('admin_promotion_create', ['id' => $produit->getId()]);
            }

            $promotion->setTitre((string) $titre);
            $promotion->setDescription((string) $description);
            $promotion->setDateDebut(new \DateTime($dateDebutStr));
            $promotion->setDateFin(new \DateTime($dateFinStr));
            $promotion->setStatut((string) $statut);

            // Calculate discount based on promotional price
            $originalPrice = $produit->getPrix();
            $promotionalPrice = (float)$prixPromotionnel;

            if ($promotionalPrice >= $originalPrice) {
                $this->addFlash('error', 'Le prix promotionnel doit etre inferieur au prix original.');
                return $this->redirectToRoute('admin_promotion_create', ['id' => $produit->getId()]);
            }

            $discountAmount = $originalPrice - $promotionalPrice;
            $discountPercentage = ($discountAmount / $originalPrice) * 100;
            $promotion->setValeurReduction($discountPercentage);

            if (!$existingPromotion) {
                $entityManager->persist($promotion);
            }
            $entityManager->flush();

            $this->addFlash('success', 'Promotion ' . ($existingPromotion ? 'modifiée' : 'créée') . ' avec succès');

            return $this->redirectToRoute('admin_promotions');
        }

        return $this->render('admin/promotions_create_fixed.html.twig', [
            'produit' => $produit,
            'existingPromotion' => $existingPromotion
        ]);
    }

    #[Route('/admin/promotion/new', name: 'admin_promotion_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $entityManager, ProduitRepository $produitRepository, \Symfony\Component\Validator\Validator\ValidatorInterface $validator): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        // Symfony Validator is injected

        if ($request->isMethod('POST')) {
            $titre = $request->request->get('titre');
            $description = $request->request->get('description');
            $valeurReduction = $request->request->get('valeur_reduction');
            $dateDebut = $request->request->get('date_debut');
            $dateFin = $request->request->get('date_fin');
            $statut = $request->request->get('statut');
            $produitId = $request->request->get('id_produit');

            $input = [
                'titre' => $titre,
                'description' => $description,
                'valeur_reduction' => $valeurReduction,
                'date_debut' => $dateDebut,
                'date_fin' => $dateFin,
                'statut' => $statut,
                'id_produit' => $produitId,
            ];

            $constraints = new \Symfony\Component\Validator\Constraints\Collection([
                'titre' => [new \Symfony\Component\Validator\Constraints\NotBlank(), new \Symfony\Component\Validator\Constraints\Length(['min' => 3])],
                'description' => [new \Symfony\Component\Validator\Constraints\NotBlank()],
                'valeur_reduction' => [new \Symfony\Component\Validator\Constraints\NotBlank(), new \Symfony\Component\Validator\Constraints\Type(['type' => 'numeric'])],
                'date_debut' => [new \Symfony\Component\Validator\Constraints\NotBlank(), new \Symfony\Component\Validator\Constraints\Date()],
                'date_fin' => [new \Symfony\Component\Validator\Constraints\NotBlank(), new \Symfony\Component\Validator\Constraints\Date()],
                'statut' => [new \Symfony\Component\Validator\Constraints\NotBlank()],
                'id_produit' => [new \Symfony\Component\Validator\Constraints\NotBlank()],
            ]);

            $violations = $validator->validate($input, $constraints);
            if (count($violations) > 0) {
                foreach ($violations as $violation) {
                    $this->addFlash('error', $violation->getMessage());
                }
                return $this->redirectToRoute('admin_promotion_new');
            }

            /** @var string $dateDebutStr */
            $dateDebutStr = (string) $dateDebut;
            /** @var string $dateFinStr */
            $dateFinStr = (string) $dateFin;
            if (new \DateTime($dateFinStr) < new \DateTime($dateDebutStr)) {
                $this->addFlash('error', 'La date de fin doit etre superieure ou egale a la date de debut.');
                return $this->redirectToRoute('admin_promotion_new');
            }

            $promotion = new Promotion();

            $promotion->setTitre((string) $titre);
            $promotion->setDescription((string) $description);
            $promotion->setValeurReduction((float)$valeurReduction);
            $promotion->setDateDebut(new \DateTime($dateDebutStr));
            $promotion->setDateFin(new \DateTime($dateFinStr));
            $promotion->setStatut((string) $statut);

            // Set product if selected
            if ($produitId) {
                $produit = $produitRepository->find($produitId);
                if ($produit) {
                    $promotion->setProduit($produit);
                }
            }

            // Set admin ID
            /** @var Utilisateur|null $currentUser */
            $currentUser = $this->getUser();
            $promotion->setIdAdmin($currentUser ? (int) $currentUser->getId() : 0);

            $entityManager->persist($promotion);
            $entityManager->flush();

            $this->addFlash('success', 'Promotion ajoutée avec succès');

            return $this->redirectToRoute('admin_promotions');
        }

        $produits = $produitRepository->findAll();

        return $this->render('admin/promotions_new_fixed.html.twig', [
            'produits' => $produits
        ]);
    }

    #[Route('/admin/promotion/{id}/edit', name: 'admin_promotion_edit', methods: ['GET', 'POST'])]
    public function edit(Promotion $promotion, Request $request, EntityManagerInterface $entityManager, ProduitRepository $produitRepository, \Symfony\Component\Validator\Validator\ValidatorInterface $validator): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        // Symfony Validator is injected

        if ($request->isMethod('POST')) {
            $titre = $request->request->get('titre');
            $description = $request->request->get('description');
            $valeurReduction = $request->request->get('valeur_reduction');
            $dateDebut = $request->request->get('date_debut');
            $dateFin = $request->request->get('date_fin');
            $statut = $request->request->get('statut');
            $produitId = $request->request->get('id_produit');

            $input = [
                'titre' => $titre,
                'description' => $description,
                'valeur_reduction' => $valeurReduction,
                'date_debut' => $dateDebut,
                'date_fin' => $dateFin,
                'statut' => $statut,
                'id_produit' => $produitId,
            ];

            $constraints = new \Symfony\Component\Validator\Constraints\Collection([
                'titre' => [new \Symfony\Component\Validator\Constraints\NotBlank(), new \Symfony\Component\Validator\Constraints\Length(['min' => 3])],
                'description' => [new \Symfony\Component\Validator\Constraints\NotBlank()],
                'valeur_reduction' => [new \Symfony\Component\Validator\Constraints\NotBlank(), new \Symfony\Component\Validator\Constraints\Type(['type' => 'numeric'])],
                'date_debut' => [new \Symfony\Component\Validator\Constraints\NotBlank(), new \Symfony\Component\Validator\Constraints\Date()],
                'date_fin' => [new \Symfony\Component\Validator\Constraints\NotBlank(), new \Symfony\Component\Validator\Constraints\Date()],
                'statut' => [new \Symfony\Component\Validator\Constraints\NotBlank()],
                'id_produit' => [new \Symfony\Component\Validator\Constraints\NotBlank()],
            ]);

            $violations = $validator->validate($input, $constraints);
            if (count($violations) > 0) {
                foreach ($violations as $violation) {
                    $this->addFlash('error', $violation->getMessage());
                }
                return $this->redirectToRoute('admin_promotion_edit', ['id' => $promotion->getId()]);
            }

            /** @var string $dateDebutStr */
            $dateDebutStr = (string) $dateDebut;
            /** @var string $dateFinStr */
            $dateFinStr = (string) $dateFin;
            $promotion->setTitre((string) $titre);
            $promotion->setDescription((string) $description);
            $promotion->setValeurReduction((float)$valeurReduction);
            $promotion->setDateDebut(new \DateTime($dateDebutStr));
            $promotion->setDateFin(new \DateTime($dateFinStr));
            $promotion->setStatut((string) $statut);

            // Update product if changed
            if ($produitId) {
                $produit = $produitRepository->find($produitId);
                if ($produit) {
                    $promotion->setProduit($produit);
                }
            } else {
                $promotion->setProduit(null);
            }

            $entityManager->flush();

            $this->addFlash('success', 'Promotion modifiée avec succès');

            return $this->redirectToRoute('admin_promotions');
        }

        $produits = $produitRepository->findAll();

        return $this->render('admin/promotions_edit_fixed.html.twig', [
            'promotion' => $promotion,
            'produits' => $produits
        ]);
    }

    #[Route('/admin/promotion/{id}/delete', name: 'admin_promotion_delete', methods: ['POST'])]
    public function delete(Promotion $promotion, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        $entityManager->remove($promotion);
        $entityManager->flush();
        
        $this->addFlash('success', 'Promotion supprimée avec succès');
        
        return $this->redirectToRoute('admin_promotions');
    }

    #[Route('/admin/promotion/{id}/toggle', name: 'admin_promotion_toggle', methods: ['POST'])]
    public function toggleStatus(Promotion $promotion, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');

        // Toggle status between active and inactive
        $newStatus = $promotion->getStatut() === 'active' ? 'inactive' : 'active';
        $promotion->setStatut($newStatus);

        $entityManager->flush();
        $this->addFlash('success', 'Statut de la promotion modifié avec succès');

        return $this->redirectToRoute('admin_promotions');
    }
}
