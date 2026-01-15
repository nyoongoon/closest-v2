import { createRouter, createWebHistory } from 'vue-router'
import MainView from "@/views/MainView.vue";

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: 'home',
      component: MainView
    }
    // ,
    // {
    //   path: "/write",
    //   name: "write",
    //   component: WriteView
    // },
    // {
    //   path: "/read/:postId",
    //   name: "read",
    //   component: ReadView,
    //   props: true,
    // },
    // {
    //   path: "/edit/:postId",
    //   name: "edit",
    //   component: EditView,
    //   props: true,
    // },
    // {
    //   path: "/example",
    //   name: "example",
    //   component: ExampleView,
    //   props: true,
    // },
    // {
    //   path: '/about',
    //   name: 'about',
    //   // route level code-splitting
    //   // this generates a separate chunk (About.[hash].js) for this route
    //   // which is lazy-loaded when the route is visited.
    //   component: () => import('../views/AboutView.vue')
    // }
  ]
})

export default router
