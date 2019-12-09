module.exports = {
  env: {
    node: true,
    es6: true,
  },
  globals: {
    Atomics: 'readonly',
    SharedArrayBuffer: 'readonly',
  },
  parserOptions: {
    ecmaVersion: 2018,
    sourceType: 'module',
  },
  rules: {
    'array-bracket-spacing': ['error', 'never'],
    'object-curly-spacing': ['error', 'always'],
    'comma-dangle': ['error', 'never'],
    'no-unused-vars': 'error',
    indent: ['error', 2],
    'object-curly-spacing': ['error', 'always'],
    'no-sequences': 'error',
    'no-debugger': 'error',
    'no-useless-escape': 'error',
    'spaced-comment': ['error', 'always'],
    // 'import/first': ['error', 'always'],
    'space-before-function-paren': ['error', 'always'],
    'semi': ["error", "never"],
    'no-trailing-spaces': ["error", { "ignoreComments": true }],
    'eol-last': ["error", "always"],
    'no-multiple-empty-lines': ["error", { "max": 2, "maxEOF": 1 }]
  },
};
