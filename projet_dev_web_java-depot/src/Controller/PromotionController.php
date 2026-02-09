<?php

namespace App\Controller;

use App\Entity\Promotion;
use App\Entity\Produit;
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
        
        return $this->render('Admin/promotions/index.html.twig', [
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
    public function createForProduct(Produit $produit, Request $request, EntityManagerInterface $entityManager): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        // Check if there's already an active promotion
        $existingPromotion = $produit->getActivePromotion();
        
        if ($request->isMethod('POST')) {
            if ($existingPromotion) {
                // Update existing promotion
                $promotion = $existingPromotion;
            } else {
                // Create new promotion
                $promotion = new Promotion();
                $promotion->setProduit($produit);
                $promotion->setIdAdmin($this->getUser()->getId());
            }
            
            $promotion->setTitre($request->request->get('titre'));
            $promotion->setDescription($request->request->get('description'));
            $promotion->setDateDebut(new \DateTime($request->request->get('date_debut')));
            $promotion->setDateFin(new \DateTime($request->request->get('date_fin')));
            $promotion->setStatut($request->request->get('statut'));
            
            // Calculate discount based on promotional price
            $originalPrice = $produit->getPrix();
            $promotionalPrice = (float)$request->request->get('prix_promotionnel');
            
            if ($promotionalPrice < $originalPrice) {
                $discountAmount = $originalPrice - $promotionalPrice;
                $discountPercentage = ($discountAmount / $originalPrice) * 100;

                $promotion->setValeurReduction($discountPercentage);
            }
            
            if (!$existingPromotion) {
                $entityManager->persist($promotion);
            }
            $entityManager->flush();
            
            $this->addFlash('success', 'Promotion ' . ($existingPromotion ? 'modifiée' : 'créée') . ' avec succès');
            
            return $this->redirectToRoute('admin_promotions');
        }
        
        return $this->render('Admin/promotions/create.html.twig', [
            'produit' => $produit,
            'existingPromotion' => $existingPromotion
        ]);
    }

    #[Route('/admin/promotion/new', name: 'admin_promotion_new', methods: ['GET', 'POST'])]
    public function new(Request $request, EntityManagerInterface $entityManager, ProduitRepository $produitRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        if ($request->isMethod('POST')) {
            $promotion = new Promotion();
            $promotion->setTitre($request->request->get('titre'));
            $promotion->setDescription($request->request->get('description'));
            $promotion->setValeurReduction((float)$request->request->get('valeur_reduction'));
            $promotion->setDateDebut(new \DateTime($request->request->get('date_debut')));
            $promotion->setDateFin(new \DateTime($request->request->get('date_fin')));
            $promotion->setStatut($request->request->get('statut'));
            
            // Set product if selected
            $produitId = $request->request->get('id_produit');
            if ($produitId) {
                $produit = $produitRepository->find($produitId);
                if ($produit) {
                    $promotion->setProduit($produit);
                }
            }
            
            // Set admin ID
            $promotion->setIdAdmin($this->getUser()->getId());
            
            $entityManager->persist($promotion);
            $entityManager->flush();
            
            $this->addFlash('success', 'Promotion ajoutée avec succès');
            
            return $this->redirectToRoute('admin_promotions');
        }
        
        $produits = $produitRepository->findAll();
        
        return $this->render('Admin/promotions/new.html.twig', [
            'produits' => $produits
        ]);
    }

    #[Route('/admin/promotion/{id}/edit', name: 'admin_promotion_edit', methods: ['GET', 'POST'])]
    public function edit(Promotion $promotion, Request $request, EntityManagerInterface $entityManager, ProduitRepository $produitRepository): Response
    {
        $this->denyAccessUnlessGranted('ROLE_ADMIN');
        
        if ($request->isMethod('POST')) {
            $promotion->setTitre($request->request->get('titre'));
            $promotion->setDescription($request->request->get('description'));
            $promotion->setValeurReduction((float)$request->request->get('valeur_reduction'));
            $promotion->setDateDebut(new \DateTime($request->request->get('date_debut')));
            $promotion->setDateFin(new \DateTime($request->request->get('date_fin')));
            $promotion->setStatut($request->request->get('statut'));
            
            // Update product if changed
            $produitId = $request->request->get('id_produit');
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
        
        return $this->render('Admin/promotions/edit.html.twig', [
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
