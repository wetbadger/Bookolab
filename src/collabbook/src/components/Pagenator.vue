<template>
  <div class="overflow-auto my-4 d-flex justify-content-center">
    <b-pagination
      v-model="currentPage"
      :total-rows="totalRows"
      :per-page="perPage"
      align="center"
    ></b-pagination>
  </div>
</template>

<script>
import { BPagination } from "bootstrap-vue-next";
import { usePageStore } from "@/stores/pageStore"; // 👈 Import your store

export default {
  name: 'Paginator',
  components: { BPagination },
  setup() {
    const pageStore = usePageStore(); // 👈 Initialize store
    return { pageStore };
  },
  data() {
    return {
      perPage: 1 // 1 row per page index calculation since totalRows represents total pages
    }
  },
  computed: {
    // 👈 ADD THIS: Pull dynamic page bounds seamlessly from your global store
    totalRows() {
      return this.pageStore.totalPages;
    },
    currentPage: {
      get() {
        return parseInt(this.$route.params.id) || 1
      },
      set(val) {
        const isEditMode = this.$route.path.endsWith('/edit')

        this.$router.push({
          name: this.$route.name,
          params: {
            ...this.$route.params,
            id: val
          },
          path: isEditMode ? `/pages/${val}/edit` : `/pages/${val}`
        })
      }
    }
  }
}
</script>
