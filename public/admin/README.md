# Curavita Healthcare Admin Portal

Pure **HTML**, **CSS**, and **JavaScript** project. No React, no build step.

## How to run

**Option 1 – Open in browser**  
Double-click `index.html` or open it from your file manager.  
(Logo and assets load from relative paths.)

**Option 2 – Local server (recommended)**  
```bash
npm start
```
Then open: **http://localhost:3000**

## Structure

- **index.html** – Single page: sidebar, header, main content (routing by hash)
- **css/styles.css** – All styles (design tokens, layout, components)
- **js/app.js** – Routing, pages (Dashboard, Products, Orders, Customers, Analytics, Settings), sidebar toggle, dropdowns, tabs, charts (Chart.js via CDN)
- **images/** – Logo and images
- **public/** – Favicon, etc.

## Pages (hash routes)

- `#/` – Dashboard  
- `#/products` – Products  
- `#/orders` – Orders  
- `#/customers` – Customers  
- `#/analytics` – Analytics  
- `#/settings` – Settings  

Design is unchanged from the original Curavita admin (green/navy, same layout and components).
