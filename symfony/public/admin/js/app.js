/* Curavita Healthcare Admin - Vanilla JS */

(function () {
  'use strict';

  const PRIMARY_GREEN = 'hsl(88, 55%, 48%)';
  const INFO_BLUE = 'hsl(199, 89%, 48%)';

  // --- Routing ---
  const routes = {
    '': { title: 'Dashboard', subtitle: "Welcome back! Here's your business overview.", render: renderDashboard },
    'products': { title: 'Products', subtitle: 'Manage your product inventory', render: renderProducts },
    'orders': { title: 'Orders', subtitle: 'Track and manage customer orders', render: renderOrders },
    'customers': { title: 'Customers', subtitle: 'Manage your customer relationships', render: renderCustomers },
    'analytics': { title: 'Analytics', subtitle: 'Business insights and performance metrics', render: renderAnalytics },
    'settings': { title: 'Settings', subtitle: 'Manage your account and preferences', render: renderSettings }
  };

  function getPath() {
    var hash = window.location.hash.slice(1) || '';
    return hash.split('/')[0].trim();
  }

  function navigate() {
    var path = getPath();
    var route = routes[path];
    var titleEl = document.getElementById('pageTitle');
    var subtitleEl = document.getElementById('pageSubtitle');
    var app = document.getElementById('app');
    if (titleEl) titleEl.textContent = route ? route.title : '404';
    if (subtitleEl) subtitleEl.textContent = route ? (route.subtitle || '') : 'Page not found';
    if (!route || !route.render) {
      if (app) app.innerHTML = render404();
      setActiveNav(path);
      return;
    }
    app.innerHTML = '';
    var content = route.render();
    if (typeof content === 'string') app.innerHTML = content;
    else if (content && content.appendChild) app.appendChild(content);
    app.classList.add('main-fade-in');
    setTimeout(function () { app.classList.remove('main-fade-in'); }, 300);
    afterRender(path);
    setActiveNav(path);
  }

  function setActiveNav(page) {
    var normalized = (page || '').trim() || 'dashboard';
    document.querySelectorAll('.nav-link').forEach(function (a) {
      var href = (a.getAttribute('href') || '').trim();
      var dataPage = a.getAttribute('data-page');
      if (!dataPage) dataPage = href === '#/' || href === '' ? 'dashboard' : href.replace(/^#\/?/, '').split('/')[0] || 'dashboard';
      a.classList.toggle('active', dataPage === normalized);
      a.setAttribute('aria-current', dataPage === normalized ? 'page' : null);
    });
  }

  // Sidebar: clic sur un lien = navigation garantie (hash + rendu)
  document.addEventListener('click', function (e) {
    var link = e.target.closest('.nav-link');
    if (!link || link.closest('.sidebar') === null) return;
    e.preventDefault();
    var href = link.getAttribute('href') || '#/';
    if (href === '#/') href = '#/';
    if (window.location.hash !== href) window.location.hash = href;
    else navigate();
  });

  window.addEventListener('hashchange', navigate);
  window.addEventListener('load', function () { navigate(); });
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', navigate);
  } else {
    navigate();
  }

  // --- Sidebar toggle ---
  (function () {
    const wrap = document.getElementById('sidebarWrap');
    const trigger = document.getElementById('sidebarTrigger');
    const stored = localStorage.getItem('sidebar');
    if (stored === 'collapsed') {
      wrap.classList.remove('expanded');
      wrap.classList.add('collapsed');
    }
    if (trigger) {
      trigger.addEventListener('click', function () {
        wrap.classList.toggle('expanded');
        wrap.classList.toggle('collapsed');
        localStorage.setItem('sidebar', wrap.classList.contains('collapsed') ? 'collapsed' : 'expanded');
      });
    }
  })();

  // --- Dropdowns: click outside to close ---
  document.addEventListener('click', function (e) {
    if (!e.target.closest('.dropdown')) {
      document.querySelectorAll('.dropdown-menu.open').forEach(function (m) { m.classList.remove('open'); });
    }
  });

  function toggleDropdown(btn) {
    const menu = btn.nextElementSibling;
    if (!menu || !menu.classList.contains('dropdown-menu')) return;
    const open = menu.classList.toggle('open');
    document.querySelectorAll('.dropdown-menu.open').forEach(function (m) {
      if (m !== menu) m.classList.remove('open');
    });
  }

  // --- After render: bind dropdowns, tabs, switches ---
  function afterRender(page) {
    document.querySelectorAll('.dropdown button, .dropdown .dropdown-trigger').forEach(function (btn) {
      btn.onclick = function (e) { e.stopPropagation(); toggleDropdown(btn); };
    });
    document.querySelectorAll('[data-tabs]').forEach(function (root) {
      var list = root.querySelector('.tabs-list');
      var contents = root.querySelectorAll('.tabs-content');
      if (!list) return;
      list.querySelectorAll('.tabs-trigger').forEach(function (trigger, i) {
        trigger.onclick = function () {
          list.querySelectorAll('.tabs-trigger').forEach(function (t) { t.classList.remove('active'); });
          trigger.classList.add('active');
          var idx = Math.min(i, contents.length - 1);
          contents.forEach(function (c, j) { c.classList.toggle('active', j === idx); });
        };
      });
    });
    document.querySelectorAll('.switch').forEach(function (el) {
      el.onclick = function () {
        el.classList.toggle('checked');
        var input = el.querySelector('input');
        if (input) input.checked = el.classList.contains('checked');
      };
    });
    if (page === 'dashboard') initDashboardCharts();
    if (page === 'analytics') initAnalyticsCharts();
  }

  // --- 404 ---
  function render404() {
    return '<div class="page-404"><div class="page-404-inner"><h1>404</h1><p class="text-muted-foreground">Oops! Page not found</p><a href="#/">Return to Home</a></div></div>';
  }

  // --- Dashboard ---
  function renderDashboard() {
    return (
      '<div class="page-content">' +
        '<div class="grid-stats">' +
          statCard('Total Revenue', '$54,239', '+12.5%', 'up', 'primary', iconDollar()) +
          statCard('Total Orders', '1,429', '+8.2%', 'up', 'info', iconCart()) +
          statCard('Active Customers', '3,842', '+15.3%', 'up', 'success', iconUsers()) +
          statCard('Products in Stock', '847', '-2.4%', 'down', 'warning', iconPackage()) +
        '</div>' +
        '<div class="grid-charts">' +
          '<div class="stat-card">' +
            '<div class="flex-between mb-6">' +
              '<div><h3 class="font-display font-semibold text-foreground">Sales Overview</h3><p class="text-sm text-muted-foreground mt-0.5">Monthly revenue trends</p></div>' +
              '<div class="flex gap-2">' +
                '<button type="button" class="btn btn-outline btn-sm">Weekly</button>' +
                '<button type="button" class="btn btn-primary-light btn-sm">Monthly</button>' +
                '<button type="button" class="btn btn-outline btn-sm">Yearly</button>' +
              '</div>' +
            '</div>' +
            '<div class="chart-container"><canvas id="chartSales"></canvas></div>' +
          '</div>' +
          '<div class="stat-card">' + topProductsHtml() + '</div>' +
        '</div>' +
        '<div class="grid-orders-activity">' +
          recentOrdersHtml() +
          activityFeedHtml() +
        '</div>' +
      '</div>'
    );
  }

  function statCard(title, value, change, trend, iconColor, iconSvg) {
    var changeHtml = '';
    if (change) {
      changeHtml = '<div class="stat-card-change ' + trend + '">' +
        (trend === 'up' ? iconTrendUp() : iconTrendDown()) +
        '<span>' + change + '</span> <span class="text-muted-foreground">vs last month</span></div>';
    }
    return '<div class="stat-card animate-fade-in">' +
      '<div class="stat-card-inner">' +
        '<div><p class="stat-card-title">' + title + '</p><p class="stat-card-value">' + value + '</p>' + changeHtml + '</div>' +
        '<div class="stat-card-icon ' + iconColor + '">' + iconSvg + '</div>' +
      '</div></div>';
  }

  function iconDollar() { return '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"/><line x1="12" x2="12" y1="2" y2="22"/><path d="M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"/></svg>'; }
  function iconCart() { return '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"/><circle cx="9" cy="21" r="1"/><circle cx="20" cy="21" r="1"/><path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/></svg>'; }
  function iconUsers() { return '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"/><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></svg>'; }
  function iconPackage() { return '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"/><path d="m7.5 4.27 9 5.15"/><path d="M21 8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16Z"/></svg>'; }
  function iconTrendUp() { return '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"/><polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/><polyline points="16 7 22 7 22 13"/></svg>'; }
  function iconTrendDown() { return '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"/><polyline points="22 17 13.5 8.5 8.5 13.5 2 7"/><polyline points="16 17 22 17 22 11"/></svg>'; }

  var topProductsData = [
    { name: 'Vitamin D3 1000 IU', sales: 1234, stock: 85, trend: '+12%' },
    { name: 'Omega-3 Fish Oil', sales: 987, stock: 62, trend: '+8%' },
    { name: 'Multivitamin Complex', sales: 876, stock: 45, trend: '+15%' },
    { name: 'Calcium + Magnesium', sales: 654, stock: 78, trend: '+5%' },
    { name: 'Probiotic Daily', sales: 543, stock: 30, trend: '+22%' }
  ];
  var maxSales = Math.max.apply(null, topProductsData.map(function (p) { return p.sales; }));

  function topProductsHtml() {
    var html = '<div class="flex-between mb-6"><div><h3 class="font-display font-semibold text-foreground">Top Products</h3><p class="text-sm text-muted-foreground mt-0.5">Best selling items this month</p></div></div><div class="space-y-5">';
    topProductsData.forEach(function (p) {
      var pct = (p.sales / maxSales) * 100;
      html += '<div class="space-y-2">' +
        '<div class="flex gap-3" style="align-items:center;justify-content:space-between">' +
          '<div class="flex gap-3" style="align-items:center">' +
            '<div class="stat-card-icon primary" style="width:2rem;height:2rem">' + iconPackage() + '</div>' +
            '<div><p class="font-medium text-foreground text-sm">' + p.name + '</p><p class="text-xs text-muted-foreground">' + p.sales + ' units sold</p></div>' +
          '</div>' +
          '<div class="text-right"><span class="text-sm font-medium" style="color:var(--success)">' + p.trend + '</span><p class="text-xs text-muted-foreground">' + p.stock + '% stock</p></div>' +
        '</div>' +
        '<div class="progress h-1\.5"><div class="progress-bar" style="width:' + pct + '%"></div></div>' +
      '</div>';
    });
    return html + '</div>';
  }

  var recentOrdersData = [
    { id: 'ORD-001', customer: 'Sarah Johnson', avatar: 'sarah', product: 'Vitamin D3 Complex', amount: '$45.99', status: 'completed', date: '2 hours ago' },
    { id: 'ORD-002', customer: 'Michael Chen', avatar: 'michael', product: 'Pain Relief Tablets', amount: '$28.50', status: 'processing', date: '4 hours ago' },
    { id: 'ORD-003', customer: 'Emma Wilson', avatar: 'emma', product: 'Allergy Medicine Pack', amount: '$67.00', status: 'pending', date: '5 hours ago' },
    { id: 'ORD-004', customer: 'James Brown', avatar: 'james', product: 'First Aid Kit Pro', amount: '$125.00', status: 'completed', date: '6 hours ago' },
    { id: 'ORD-005', customer: 'Lisa Martinez', avatar: 'lisa', product: 'Omega-3 Supplements', amount: '$39.99', status: 'shipped', date: '8 hours ago' }
  ];
  var statusBadgeClass = { completed: 'badge-success', processing: 'badge-info', pending: 'badge-warning', shipped: 'badge-primary' };

  function recentOrdersHtml() {
    var html = '<div class="stat-card p-0 overflow-hidden">' +
      '<div class="card-header-border flex-between">' +
        '<div><h3 class="font-display font-semibold text-foreground">Recent Orders</h3><p class="text-sm text-muted-foreground mt-0.5">Latest customer transactions</p></div>' +
        '<button type="button" class="btn btn-outline btn-sm">View All</button></div>' +
      '<div class="recent-orders-list">';
    recentOrdersData.forEach(function (o) {
      html += '<div class="recent-order-row">' +
        '<div class="avatar"><img src="https://api.dicebear.com/7.x/avataaars/svg?seed=' + o.avatar + '" alt=""/></div>' +
        '<div class="recent-order-body">' +
          '<div class="flex gap-2" style="align-items:center"><p class="font-medium text-foreground truncate">' + o.customer + '</p><span class="text-xs text-muted-foreground hidden-sm">' + o.id + '</span></div>' +
          '<p class="text-sm text-muted-foreground truncate">' + o.product + '</p></div>' +
        '<div class="hidden-md" style="text-align:right"><p class="font-semibold text-foreground">' + o.amount + '</p><p class="text-xs text-muted-foreground">' + o.date + '</p></div>' +
        '<span class="badge ' + (statusBadgeClass[o.status] || 'badge-muted') + ' capitalize">' + o.status + '</span>' +
        '<button type="button" class="btn btn-ghost btn-icon btn-sm"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"/><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg></button>' +
      '</div>';
    });
    return html + '</div></div>';
  }

  var activities = [
    { msg: 'New order #ORD-006 received', time: '2 minutes ago', color: 'primary', icon: 'cart' },
    { msg: 'New customer registered', time: '15 minutes ago', color: 'info', icon: 'user-plus' },
    { msg: 'Low stock alert: Vitamin C', time: '1 hour ago', color: 'warning', icon: 'alert' },
    { msg: 'Order #ORD-004 shipped', time: '2 hours ago', color: 'success', icon: 'truck' },
    { msg: 'Product updated: Pain Relief', time: '3 hours ago', color: 'muted', icon: 'package' },
    { msg: 'Order #ORD-003 completed', time: '4 hours ago', color: 'success', icon: 'check' }
  ];

  function activityFeedHtml() {
    var html = '<div class="stat-card"><div class="flex-between mb-6"><div><h3 class="font-display font-semibold text-foreground">Recent Activity</h3><p class="text-sm text-muted-foreground mt-0.5">Latest system events</p></div></div><div class="space-y-4">';
    activities.forEach(function (a) {
      var iconCl = 'stat-card-icon ' + a.color;
      html += '<div class="flex gap-3 animate-fade-in" style="align-items:flex-start">' +
        '<div class="' + iconCl + '" style="width:2.25rem;height:2.25rem;display:flex;align-items:center;justify-content:center"></div>' +
        '<div style="flex:1;min-width:0"><p class="text-sm font-medium text-foreground">' + a.msg + '</p><p class="text-xs text-muted-foreground mt-0.5">' + a.time + '</p></div>' +
      '</div>';
    });
    return html + '</div></div>';
  }

  var salesChartData = [
    { name: 'Jan', sales: 4000 },
    { name: 'Feb', sales: 3000 },
    { name: 'Mar', sales: 5000 },
    { name: 'Apr', sales: 4500 },
    { name: 'May', sales: 6000 },
    { name: 'Jun', sales: 5500 },
    { name: 'Jul', sales: 7000 }
  ];

  function initDashboardCharts() {
    var canvas = document.getElementById('chartSales');
    if (!canvas || typeof Chart === 'undefined') return;
    new Chart(canvas.getContext('2d'), {
      type: 'line',
      data: {
        labels: salesChartData.map(function (d) { return d.name; }),
        datasets: [{
          label: 'Sales',
          data: salesChartData.map(function (d) { return d.sales; }),
          borderColor: PRIMARY_GREEN,
          backgroundColor: 'rgba(88, 55%, 48%, 0.2)',
          fill: true,
          tension: 0.3
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: { legend: { display: false } },
        scales: {
          x: { grid: { display: false }, ticks: { color: 'hsl(215, 20%, 45%)' } },
          y: { grid: { color: 'hsl(215, 20%, 90%)' }, ticks: { color: 'hsl(215, 20%, 45%)', callback: function (v) { return '$' + (v / 1000) + 'k'; } } }
        }
      }
    });
  }

  // --- Products ---
  var productsData = [
    { id: 'PRD-001', name: 'Vitamin D3 1000 IU', category: 'Vitamins', price: '$24.99', stock: 234, status: 'In Stock' },
    { id: 'PRD-002', name: 'Omega-3 Fish Oil', category: 'Supplements', price: '$34.99', stock: 156, status: 'In Stock' },
    { id: 'PRD-003', name: 'Multivitamin Complex', category: 'Vitamins', price: '$29.99', stock: 89, status: 'Low Stock' },
    { id: 'PRD-004', name: 'Pain Relief Tablets', category: 'Medicine', price: '$12.99', stock: 0, status: 'Out of Stock' },
    { id: 'PRD-005', name: 'Allergy Medicine Pack', category: 'Medicine', price: '$45.99', stock: 432, status: 'In Stock' },
    { id: 'PRD-006', name: 'First Aid Kit Pro', category: 'Equipment', price: '$89.99', stock: 67, status: 'In Stock' },
    { id: 'PRD-007', name: 'Probiotic Daily', category: 'Supplements', price: '$39.99', stock: 23, status: 'Low Stock' },
    { id: 'PRD-008', name: 'Calcium + Magnesium', category: 'Minerals', price: '$19.99', stock: 198, status: 'In Stock' }
  ];
  var productStatusClass = { 'In Stock': 'badge-success', 'Low Stock': 'badge-warning', 'Out of Stock': 'badge-destructive' };

  function renderProducts() {
    var rows = productsData.map(function (p) {
      return '<tr class="hover:bg-secondary" style="border-bottom:1px solid var(--border)">' +
        '<td style="padding:1rem"><div class="flex gap-3" style="align-items:center">' +
          '<div class="stat-card-icon primary" style="width:2.5rem;height:2.5rem">' + iconPackage() + '</div>' +
          '<div><p class="font-medium text-foreground">' + p.name + '</p><p class="text-xs text-muted-foreground">' + p.id + '</p></div></div></td>' +
        '<td class="text-muted-foreground">' + p.category + '</td>' +
        '<td class="font-medium">' + p.price + '</td>' +
        '<td class="text-muted-foreground">' + p.stock + ' units</td>' +
        '<td><span class="badge ' + (productStatusClass[p.status] || 'badge-muted') + '">' + p.status + '</span></td>' +
        '<td class="text-right">' +
          '<div class="dropdown">' +
            '<button type="button" class="btn btn-ghost btn-icon dropdown-trigger"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"/><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/><circle cx="5" cy="12" r="1"/></svg></button>' +
            '<div class="dropdown-menu">' +
              '<button type="button" class="dropdown-item"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"/> View Details</button>' +
              '<button type="button" class="dropdown-item"> Edit Product</button>' +
              '<button type="button" class="dropdown-item destructive"> Delete</button>' +
            '</div></div></td></tr>';
    }).join('');
    return '<div class="page-content">' +
      '<div class="flex-between flex-col sm:flex-row gap-4">' +
        '<div class="flex gap-3" style="flex:1">' +
          '<div class="input-wrap" style="flex:1;max-width:28rem">' +
            '<svg class="input-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"/><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>' +
            '<input type="search" placeholder="Search products..." class="input" style="padding-left:2.5rem" />' +
          '</div>' +
          '<button type="button" class="btn btn-outline"> Filters</button>' +
        '</div>' +
        '<button type="button" class="btn btn-primary"> Add Product</button>' +
      '</div>' +
      '<div class="stat-card p-0 overflow-hidden">' +
        '<div class="table-wrap"><table><thead><tr class="table-row-head">' +
          '<th class="font-semibold">Product</th><th class="font-semibold">Category</th><th class="font-semibold">Price</th><th class="font-semibold">Stock</th><th class="font-semibold">Status</th><th class="font-semibold text-right">Actions</th>' +
        '</tr></thead><tbody>' + rows + '</tbody></table></div>' +
      '</div></div>';
  }

  // --- Orders ---
  var ordersData = [
    { id: 'ORD-001', customer: 'Sarah Johnson', avatar: 'sarah', items: 3, total: '$145.99', date: 'Jan 28, 2026', status: 'completed' },
    { id: 'ORD-002', customer: 'Michael Chen', avatar: 'michael', items: 1, total: '$28.50', date: 'Jan 28, 2026', status: 'processing' },
    { id: 'ORD-003', customer: 'Emma Wilson', avatar: 'emma', items: 5, total: '$267.00', date: 'Jan 27, 2026', status: 'pending' },
    { id: 'ORD-004', customer: 'James Brown', avatar: 'james', items: 2, total: '$125.00', date: 'Jan 27, 2026', status: 'shipped' },
    { id: 'ORD-005', customer: 'Lisa Martinez', avatar: 'lisa', items: 4, total: '$189.99', date: 'Jan 26, 2026', status: 'completed' },
    { id: 'ORD-006', customer: 'David Kim', avatar: 'david', items: 1, total: '$34.99', date: 'Jan 26, 2026', status: 'cancelled' },
    { id: 'ORD-007', customer: 'Anna Smith', avatar: 'anna', items: 2, total: '$78.50', date: 'Jan 25, 2026', status: 'completed' }
  ];
  var orderStatusClass = { completed: 'badge-success', processing: 'badge-info', pending: 'badge-warning', shipped: 'badge-primary', cancelled: 'badge-destructive' };

  function renderOrders() {
    var rows = ordersData.map(function (o) {
      return '<tr style="border-bottom:1px solid var(--border)">' +
        '<td class="font-medium text-primary">' + o.id + '</td>' +
        '<td><div class="flex gap-3" style="align-items:center"><div class="avatar"><img src="https://api.dicebear.com/7.x/avataaars/svg?seed=' + o.avatar + '" alt=""/></div><span class="font-medium">' + o.customer + '</span></div></td>' +
        '<td class="text-muted-foreground">' + o.items + ' items</td><td class="font-semibold">' + o.total + '</td>' +
        '<td class="text-muted-foreground">' + o.date + '</td>' +
        '<td><span class="badge ' + (orderStatusClass[o.status] || 'badge-muted') + ' capitalize">' + o.status + '</span></td>' +
        '<td class="text-right">' +
          '<div class="dropdown">' +
            '<button type="button" class="btn btn-ghost btn-icon dropdown-trigger"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="1"/><circle cx="19" cy="12" r="1"/><circle cx="5" cy="12" r="1"/></svg></button>' +
            '<div class="dropdown-menu">' +
              '<button type="button" class="dropdown-item"> View Details</button>' +
              '<button type="button" class="dropdown-item"> Update Status</button>' +
              '<button type="button" class="dropdown-item"> Print Invoice</button>' +
            '</div></div></td></tr>';
    }).join('');
    return '<div class="page-content" data-tabs="">' +
      '<div class="flex-between flex-col sm:flex-row gap-4">' +
        '<div class="tabs-list">' +
          '<button type="button" class="tabs-trigger active">All Orders</button>' +
          '<button type="button" class="tabs-trigger">Pending</button>' +
          '<button type="button" class="tabs-trigger">Processing</button>' +
          '<button type="button" class="tabs-trigger">Completed</button>' +
        '</div>' +
        '<div class="flex gap-3">' +
          '<div class="input-wrap" style="width:100%;max-width:16rem">' +
            '<svg class="input-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"/><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg>' +
            '<input type="search" placeholder="Search orders..." class="input" style="padding-left:2.5rem" />' +
          '</div>' +
          '<button type="button" class="btn btn-outline"> Filters</button>' +
        '</div>' +
      '</div>' +
      '<div class="tabs-content active mt-6">' +
        '<div class="stat-card p-0 overflow-hidden">' +
          '<div class="table-wrap"><table><thead><tr class="table-row-head">' +
            '<th class="font-semibold">Order ID</th><th class="font-semibold">Customer</th><th class="font-semibold">Items</th><th class="font-semibold">Total</th><th class="font-semibold">Date</th><th class="font-semibold">Status</th><th class="font-semibold text-right">Actions</th>' +
          '</tr></thead><tbody>' + rows + '</tbody></table></div>' +
        '</div>' +
      '</div></div>';
  }

  // --- Customers ---
  var customersData = [
    { id: 1, name: 'Sarah Johnson', email: 'sarah@email.com', phone: '+1 234 567 890', location: 'New York, USA', orders: 12, spent: '$1,234.50', status: 'active', avatar: 'sarah' },
    { id: 2, name: 'Michael Chen', email: 'michael@email.com', phone: '+1 345 678 901', location: 'Los Angeles, USA', orders: 8, spent: '$876.00', status: 'active', avatar: 'michael' },
    { id: 3, name: 'Emma Wilson', email: 'emma@email.com', phone: '+44 20 7123 4567', location: 'London, UK', orders: 24, spent: '$3,456.99', status: 'vip', avatar: 'emma' },
    { id: 4, name: 'James Brown', email: 'james@email.com', phone: '+1 456 789 012', location: 'Chicago, USA', orders: 5, spent: '$432.00', status: 'active', avatar: 'james' },
    { id: 5, name: 'Lisa Martinez', email: 'lisa@email.com', phone: '+34 91 123 4567', location: 'Madrid, Spain', orders: 3, spent: '$189.99', status: 'new', avatar: 'lisa' },
    { id: 6, name: 'David Kim', email: 'david@email.com', phone: '+82 2 1234 5678', location: 'Seoul, Korea', orders: 0, spent: '$0.00', status: 'inactive', avatar: 'david' }
  ];
  var customerStatusClass = { active: 'badge-success', vip: 'badge-primary', new: 'badge-info', inactive: 'badge-muted' };

  function renderCustomers() {
    var cards = customersData.map(function (c) {
      return '<div class="card stat-card p-5">' +
        '<div class="flex-between mb-4">' +
          '<div class="flex gap-3" style="align-items:center">' +
            '<div class="avatar avatar-lg"><img src="https://api.dicebear.com/7.x/avataaars/svg?seed=' + c.avatar + '" alt=""/></div>' +
            '<div><h3 class="font-semibold text-foreground">' + c.name + '</h3><span class="badge ' + (customerStatusClass[c.status] || 'badge-muted') + ' capitalize text-xs">' + c.status + '</span></div>' +
          '</div>' +
          '<div class="dropdown"><button type="button" class="btn btn-ghost btn-icon btn-sm dropdown-trigger text-muted-foreground"></button>' +
          '<div class="dropdown-menu"><button type="button" class="dropdown-item"> View Profile</button><button type="button" class="dropdown-item"> Edit</button><button type="button" class="dropdown-item destructive"> Deactivate</button></div></div>' +
        '</div>' +
        '<div class="space-y-2 text-sm">' +
          '<div class="flex gap-2 text-muted-foreground" style="align-items:center"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"/><rect width="20" height="16" x="2" y="4" rx="2"/><path d="m22 7-8.97 5.7a1.94 1.94 0 0 1-2.06 0L2 7"/></svg><span class="truncate">' + c.email + '</span></div>' +
          '<div class="flex gap-2 text-muted-foreground" style="align-items:center"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"/><path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7A2 2 0 0 1 22 16.92z"/></svg><span>' + c.phone + '</span></div>' +
          '<div class="flex gap-2 text-muted-foreground" style="align-items:center"><svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"/><path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0Z"/><circle cx="12" cy="10" r="3"/></svg><span>' + c.location + '</span></div>' +
        '</div>' +
        '<div class="flex-between mt-5 pt-4" style="border-top:1px solid var(--border)">' +
          '<div class="text-center"><p class="text-lg font-semibold text-foreground" style="margin:0">' + c.orders + '</p><p class="text-xs text-muted-foreground" style="margin:0">Orders</p></div>' +
          '<div class="text-center"><p class="text-lg font-semibold text-primary" style="margin:0">' + c.spent + '</p><p class="text-xs text-muted-foreground" style="margin:0">Total Spent</p></div>' +
        '</div></div>';
    }).join('');
    return '<div class="page-content">' +
      '<div class="flex-between flex-col sm:flex-row gap-4">' +
        '<div class="flex gap-3" style="flex:1">' +
          '<div class="input-wrap" style="flex:1;max-width:28rem"><svg class="input-icon" xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"/><circle cx="11" cy="11" r="8"/><path d="m21 21-4.3-4.3"/></svg><input type="search" placeholder="Search customers..." class="input" style="padding-left:2.5rem" /></div>' +
          '<button type="button" class="btn btn-outline"> Filters</button>' +
        '</div>' +
        '<button type="button" class="btn btn-primary"> Add Customer</button>' +
      '</div>' +
      '<div class="grid-stats" style="grid-template-columns:repeat(auto-fill,minmax(280px,1fr))">' + cards + '</div></div>';
  }

  // --- Analytics ---
  var revenueData = [
    { name: 'Week 1', revenue: 12000, orders: 145 },
    { name: 'Week 2', revenue: 15000, orders: 178 },
    { name: 'Week 3', revenue: 11000, orders: 132 },
    { name: 'Week 4', revenue: 18000, orders: 210 }
  ];
  var categoryData = [
    { name: 'Vitamins', value: 35, color: 'hsl(152, 76%, 36%)' },
    { name: 'Supplements', value: 25, color: INFO_BLUE },
    { name: 'Medicine', value: 20, color: 'hsl(38, 92%, 50%)' },
    { name: 'Equipment', value: 12, color: 'hsl(280, 60%, 50%)' },
    { name: 'Other', value: 8, color: 'hsl(210, 15%, 60%)' }
  ];
  var trafficData = [
    { name: 'Mon', desktop: 4000, mobile: 2400 },
    { name: 'Tue', desktop: 3000, mobile: 1398 },
    { name: 'Wed', desktop: 2000, mobile: 9800 },
    { name: 'Thu', desktop: 2780, mobile: 3908 },
    { name: 'Fri', desktop: 1890, mobile: 4800 },
    { name: 'Sat', desktop: 2390, mobile: 3800 },
    { name: 'Sun', desktop: 3490, mobile: 4300 }
  ];

  function renderAnalytics() {
    return '<div class="page-content">' +
      '<div class="flex-between flex-col sm:flex-row gap-4">' +
        '<div class="flex gap-2"><button type="button" class="btn btn-outline"> Last 30 Days</button></div>' +
        '<button type="button" class="btn btn-outline"> Export Report</button>' +
      '</div>' +
      '<div class="grid-stats">' +
        statCard('Revenue Growth', '+24.5%', '+5.2%', 'up', 'primary', iconTrendUp()) +
        statCard('Conversion Rate', '3.42%', '+0.8%', 'up', 'success', iconCart()) +
        statCard('Page Views', '48.2K', '-2.1%', 'down', 'info', '<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"/><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg>') +
        statCard('New Customers', '1,284', '+12.3%', 'up', 'warning', iconUsers()) +
      '</div>' +
      '<div class="grid-charts" style="grid-template-columns:1fr 1fr">' +
        '<div class="stat-card"><div class="flex-between mb-6"><div><h3 class="font-display font-semibold text-foreground">Revenue Overview</h3><p class="text-sm text-muted-foreground mt-0.5">Weekly breakdown</p></div></div><div class="chart-container chart-container-280"><canvas id="chartRevenue"></canvas></div></div>' +
        '<div class="stat-card"><div class="flex-between mb-6"><div><h3 class="font-display font-semibold text-foreground">Sales by Category</h3><p class="text-sm text-muted-foreground mt-0.5">Product distribution</p></div></div><div class="chart-container chart-container-280"><canvas id="chartPie"></canvas></div></div>' +
      '</div>' +
      '<div class="stat-card"><div class="flex-between mb-6"><div><h3 class="font-display font-semibold text-foreground">Traffic by Device</h3><p class="text-sm text-muted-foreground mt-0.5">Desktop vs Mobile visitors</p></div></div><div class="chart-container"><canvas id="chartTraffic"></canvas></div></div>' +
    '</div>';
  }

  function initAnalyticsCharts() {
    if (typeof Chart === 'undefined') return;
    var rev = document.getElementById('chartRevenue');
    if (rev) {
      new Chart(rev.getContext('2d'), {
        type: 'line',
        data: {
          labels: revenueData.map(function (d) { return d.name; }),
          datasets: [{ label: 'Revenue', data: revenueData.map(function (d) { return d.revenue; }), borderColor: PRIMARY_GREEN, backgroundColor: 'rgba(88, 55%, 48%, 0.2)', fill: true, tension: 0.3 }]
        },
        options: { responsive: true, maintainAspectRatio: false, plugins: { legend: { display: false } }, scales: { x: { grid: { display: false } }, y: { ticks: { callback: function (v) { return '$' + (v / 1000) + 'k'; } } } } }
      });
    }
    var pie = document.getElementById('chartPie');
    if (pie) {
      new Chart(pie.getContext('2d'), {
        type: 'doughnut',
        data: {
          labels: categoryData.map(function (d) { return d.name; }),
          datasets: [{ data: categoryData.map(function (d) { return d.value; }), backgroundColor: categoryData.map(function (d) { return d.color; }), borderWidth: 0 }]
        },
        options: { responsive: true, maintainAspectRatio: false, cutout: '60%', plugins: { legend: { position: 'right' } } }
      });
    }
    var traffic = document.getElementById('chartTraffic');
    if (traffic) {
      new Chart(traffic.getContext('2d'), {
        type: 'bar',
        data: {
          labels: trafficData.map(function (d) { return d.name; }),
          datasets: [
            { label: 'Desktop', data: trafficData.map(function (d) { return d.desktop; }), backgroundColor: PRIMARY_GREEN, borderRadius: 4 },
            { label: 'Mobile', data: trafficData.map(function (d) { return d.mobile; }), backgroundColor: INFO_BLUE, borderRadius: 4 }
          ]
        },
        options: { responsive: true, maintainAspectRatio: false, scales: { x: { grid: { display: false } }, y: { grid: { color: 'hsl(215, 20%, 90%)' } } } }
      });
    }
  }

  // --- Settings ---
  function renderSettings() {
    return '<div class="page-content" style="max-width:56rem" data-tabs="">' +
      '<div class="tabs-list mb-6">' +
        '<button type="button" class="tabs-trigger active"> Profile</button>' +
        '<button type="button" class="tabs-trigger"> Notifications</button>' +
        '<button type="button" class="tabs-trigger"> Security</button>' +
        '<button type="button" class="tabs-trigger"> Billing</button>' +
      '</div>' +
      '<div class="tabs-content active">' +
        '<div class="card stat-card">' +
          '<h3 class="font-display font-semibold text-foreground mb-6">Profile Information</h3>' +
          '<div class="flex gap-6 mb-8" style="align-items:center">' +
            '<div style="position:relative"><div class="avatar avatar-xl" style="border:4px solid var(--border)"><img src="https://api.dicebear.com/7.x/avataaars/svg?seed=admin" alt=""/></div><button type="button" class="btn btn-primary btn-icon" style="position:absolute;bottom:-4px;right:-4px;width:2rem;height:2rem;border-radius:9999px"></button></div>' +
            '<div><h4 class="font-medium text-foreground">Profile Photo</h4><p class="text-sm text-muted-foreground">JPG, PNG or GIF. Max 2MB.</p></div>' +
          '</div>' +
          '<div style="display:grid;gap:1.5rem;grid-template-columns:repeat(2,1fr)">' +
            '<div><label class="label" for="firstName">First Name</label><input id="firstName" class="input" value="Admin" /></div>' +
            '<div><label class="label" for="lastName">Last Name</label><input id="lastName" class="input" value="User" /></div>' +
            '<div><label class="label" for="email">Email Address</label><input id="email" type="email" class="input" value="admin@medadmin.com" /></div>' +
            '<div><label class="label" for="phone">Phone Number</label><input id="phone" type="tel" class="input" value="+1 234 567 890" /></div>' +
            '<div style="grid-column:span 2"><label class="label" for="bio">Bio</label><input id="bio" class="input" placeholder="Tell us about yourself..." /></div>' +
          '</div>' +
          '<div class="flex" style="justify-content:flex-end;margin-top:1.5rem"><button type="button" class="btn btn-primary">Save Changes</button></div>' +
        '</div>' +
      '</div>' +
      '<div class="tabs-content">' +
        '<div class="card stat-card">' +
          '<h3 class="font-display font-semibold text-foreground mb-6">Notification Preferences</h3>' +
          '<div class="space-y-6">' +
            '<div class="flex-between"><div class="flex gap-3"><div class="stat-card-icon primary">📧</div><div><p class="font-medium text-foreground">Email Notifications</p><p class="text-sm text-muted-foreground">Receive order updates via email</p></div></div><div class="switch checked"><div class="switch-thumb"></div></div></div>' +
            '<div class="separator"></div>' +
            '<div class="flex-between"><div class="flex gap-3"><div class="stat-card-icon info">🔔</div><div><p class="font-medium text-foreground">Push Notifications</p><p class="text-sm text-muted-foreground">Browser push notifications</p></div></div><div class="switch checked"><div class="switch-thumb"></div></div></div>' +
            '<div class="separator"></div>' +
            '<div class="flex-between"><div class="flex gap-3"><div class="stat-card-icon warning">🌐</div><div><p class="font-medium text-foreground">Marketing Updates</p><p class="text-sm text-muted-foreground">News and promotional content</p></div></div><div class="switch"><div class="switch-thumb"></div></div></div>' +
          '</div></div>' +
      '</div>' +
      '<div class="tabs-content">' +
        '<div class="card stat-card"><h3 class="font-display font-semibold text-foreground mb-6">Change Password</h3>' +
          '<div class="space-y-4" style="max-width:28rem">' +
            '<div><label class="label" for="currentPassword">Current Password</label><input id="currentPassword" type="password" class="input" /></div>' +
            '<div><label class="label" for="newPassword">New Password</label><input id="newPassword" type="password" class="input" /></div>' +
            '<div><label class="label" for="confirmPassword">Confirm New Password</label><input id="confirmPassword" type="password" class="input" /></div>' +
          '</div><div style="margin-top:1.5rem"><button type="button" class="btn btn-primary">Update Password</button></div></div>' +
        '<div class="card stat-card mt-6"><h3 class="font-display font-semibold text-foreground mb-6">Two-Factor Authentication</h3>' +
          '<div class="flex-between"><div><p class="font-medium text-foreground">Enable 2FA</p><p class="text-sm text-muted-foreground">Add an extra layer of security to your account</p></div><div class="switch"><div class="switch-thumb"></div></div></div></div>' +
      '</div>' +
      '<div class="tabs-content">' +
        '<div class="card stat-card"><h3 class="font-display font-semibold text-foreground mb-6">Current Plan</h3>' +
          '<div class="billing-plan-card flex-between"><div><p class="font-semibold text-foreground">Professional Plan</p><p class="text-sm text-muted-foreground">$49/month • Billed monthly</p></div><button type="button" class="btn btn-outline">Upgrade Plan</button></div></div>' +
        '<div class="card stat-card mt-6"><h3 class="font-display font-semibold text-foreground mb-6">Payment Method</h3>' +
          '<div class="flex-between p-4 rounded-lg border" style="border-color:var(--border)"><div class="flex gap-3"><div class="visa-card">VISA</div><div><p class="font-medium text-foreground">•••• •••• •••• 4242</p><p class="text-sm text-muted-foreground">Expires 12/26</p></div></div><button type="button" class="btn btn-outline btn-sm">Edit</button></div>' +
          '<div style="margin-top:1rem"><button type="button" class="btn btn-outline"> Add Payment Method</button></div></div>' +
      '</div>' +
    '</div>';
  }
})();
