INSERT INTO funcionarios(nome, senha, cargo, agencia)
VALUES (
        'Davi França',
        '$2b$10$nE4o6ciWHaU7a4gilMzn0.YqKXL/ltPLIr9ZYK3LkhdwUVqPl1DgC',
        'Gerente',
        '001'
)
ON CONFLICT (nome) DO NOTHING;