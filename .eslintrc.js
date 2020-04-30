module.exports = {
  env: {
    node: true,
    es6: true
  },
  extends: ['plugin:import/errors'],
  globals: {
    Atomics: 'readonly',
    SharedArrayBuffer: 'readonly'
  },
  parser: 'babel-eslint',
  parserOptions: {
    sourceType: 'module'
  },
  rules: {
    'array-bracket-spacing': ['error', 'never'],
    'object-curly-spacing': ['error', 'always'],
    'comma-dangle': ['error', 'never'],
    'no-console': 'error',
    'no-unused-vars': 'error',
    indent: ['error', 2],
    'object-curly-spacing': ['error', 'always'],
    'no-sequences': 'error',
    'no-debugger': 'error',
    'no-useless-escape': 'error',
    'spaced-comment': ['error', 'always'],
    'import/first': ['error'],
    'space-before-function-paren': ['error', 'always'],
    semi: ['error', 'never'],
    'no-trailing-spaces': [
      'error',
      { ignoreComments: true }
    ],
    'eol-last': ['error', 'always'],
    'no-multiple-empty-lines': [
      'error',
      { max: 2, maxEOF: 1 }
    ],
    'no-undef': 'error'
  }
}
