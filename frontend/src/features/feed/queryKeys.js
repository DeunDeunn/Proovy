export const feedKeys = {
  all: ["feed"],
  publics: () => [...feedKeys.all, "public"],
  public: (params) => [...feedKeys.publics(), params],
};
