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
import {BPagination} from "bootstrap-vue-next";

export default {
  name: 'Paginator',
  components: {BPagination},
  data() {
    return {
      perPage: 1,
      totalRows: 3, // Adjust this number if you have more than 3 pages total
    }
  },
  computed: {
    currentPage: {
      // 1. Read 'id' from the URL path parameter (/pages/:id)
      get() {
        return parseInt(this.$route.params.id) || 1
      },
      // 2. Route to the new page path when clicked
      set(val) {
        const isEditMode = this.$route.path.endsWith('/edit')

        this.$router.push({
          name: this.$route.name,
          params: {
            ...this.$route.params,
            id: val // Updates the dynamic :id parameter in your router
          },
          path: isEditMode ? `/pages/${val}/edit` : `/pages/${val}`
        })
      }
    }
  }
}
</script>
