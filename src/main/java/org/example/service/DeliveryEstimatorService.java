package org.example.service;

import org.example.model.Commande;

import java.time.LocalDateTime;

public class DeliveryEstimatorService {
    public LocalDateTime estimateDeliveryDate(Commande order) {
        int hours = 48;

        String address = order.getAdresseLivraison() == null ? "" : order.getAdresseLivraison().toLowerCase();
        if (!address.isBlank() && !address.contains("tunis")) {
            hours += 24;
        }

        if (order.getBaseShippingCost() >= 15) {
            hours += 12;
        }

        if ("review".equalsIgnoreCase(order.getStatut())) {
            hours += 24;
        }

        return LocalDateTime.now().plusHours(hours);
    }

    public LocalDateTime estimateDeliveryDateWithMaps(Commande order, MapsApiService mapsApiService) {
        int extraHours = 0;
        MapsApiService.GeocodeResult geo = mapsApiService.geocodeAddress(order.getAdresseLivraison());
        if (geo.found()) {
            double tunisLat = 36.8065;
            double tunisLon = 10.1815;
            double km = haversineKm(tunisLat, tunisLon, geo.latitude(), geo.longitude());
            if (km > 200) {
                extraHours += 24;
            }
            if (km > 400) {
                extraHours += 12;
            }
        }

        return estimateDeliveryDate(order).plusHours(extraHours);
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double r = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return r * c;
    }
}
