/**
 * Copyright (c) 2017-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

// See https://docusaurus.io/docs/site-config for all the possible
// site configuration options.

// List of projects/orgs using your project for the users page.
const users = [];

const siteConfig = {
  title: "inspectIT Ocelot Documentation", // Title for your website.
  tagline: "Documentation of the inspectIT Ocelot Java Agent",
  url: "https://inspectit.github.io", // Your website URL
  baseUrl: "/inspectit-ocelot/",
  //baseUrl: '/inspectit-ocelot/', // Base URL for your project */
  // For github.io type URLs, you would set the url and baseUrl like:
  //   url: 'https://facebook.github.io',
  //   baseUrl: '/test-site/',

  // Used for publishing and more
  projectName: "inspectit-ocelot",
  organizationName: "inspectIT",
  // For top-level user or org sites, the organization is still the same.
  // e.g., for the https://JoelMarcey.github.io site, it would be set like...
  //   organizationName: 'JoelMarcey'

  // For no header links in the top nav bar -> headerLinks: [],
  headerLinks: [
    { href: "https://inspectit.rocks/", label: "inspectIT Ocelot Website" },
    {
      href: "https://github.com/inspectIT/inspectit-ocelot",
      label: "GitHub Repository",
    },
  ],

  // If you have users set above, you add it here:
  users,

  /* path to images for header/footer */
  headerIcon: "img/ocelot_head_sil_logo.svg",
  footerIcon: "img/ocelot_head_sil_logo.svg",
  favicon: "img/favicon.ico",

  /* Colors for website */
  colors: {
    primaryColor: "#de6f00",
    secondaryColor: "#126e14",
  },

  /* Custom fonts for website */
  /*
  fonts: {
    myFont: [
      "Times New Roman",
      "Serif"
    ],
    myOtherFont: [
      "-apple-system",
      "system-ui"
    ]
  },
  */

  // This copyright info is used in /core/Footer.js and blog RSS/Atom feeds.
  copyright: `Copyright © ${new Date().getFullYear()} Novatec Consulting GmbH`,

  highlight: {
    theme: "atom-one-dark",
  },

  // Add custom scripts here that would be placed in <script> tags.
  scripts: ["https://buttons.github.io/buttons.js"],

  // On page navigation for the current documentation page.
  onPageNav: "separate",
  // No .html extensions for paths.
  cleanUrl: true,

  // Open Graph and Twitter card images.
  ogImage: "img/undraw_online.svg",
  twitterImage: "img/undraw_tweetstorm.svg",

  // Show documentation's last contributor's name.
  enableUpdateBy: true,

  // Show documentation's last update time.
  enableUpdateTime: true,

  scrollToTop: true,
  scrollToTopOptions: {
    zIndex: 100,
  },

  // You may provide arbitrary config keys to be used as needed by your
  // template. For example, if you need your repo's URL...
  // repoUrl: 'https://github.com/facebook/test-site',

  docsSideNavCollapsible: true,

  editUrl:
    "https://github.com/inspectIT/inspectit-ocelot/edit/master/inspectit-ocelot-documentation/docs/",

  algolia: {
    apiKey: "d499024131bf8d92e6d469a6c14f4798",
    appId: "P7JV0ZK66K",
    indexName: "inspectit-ocelot",
    algoliaOptions: {
      facetFilters: ["version:VERSION"],
    },
  },

  markdownPlugins: [
    // Highlight admonitions.
    require("remarkable-admonitions")({ icon: "svg-inline" }),
  ],
};

module.exports = siteConfig;
