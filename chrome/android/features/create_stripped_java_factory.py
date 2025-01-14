#!/usr/bin/env python
#
# Copyright 2019 The Chromium Authors. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

"""Generates a stripped down version of a java factory file.

A stripped down factory file is required in a feature's public_java target
during the compilation process so that features can depend on each other
without creating circular dependencies.

Afterwards, the stripped down factory's .class file is excluded from the
resulting target. The real factory uses the feature's internal implementations,
which is why it is not included in the feature's public_java target.

This script generates a stripped down factory file from real factory file to
reduce the burden of maintenance. The stripped down factory will have dummy
implementations of all public methods of the real factory.

This script requires that the real factory file has exactly one top-level class.
"""

import argparse
import datetime
import sys
import os

sys.path.append(os.path.join(os.path.dirname(__file__),
                             os.pardir, os.pardir, os.pardir,
                             'build', 'android', 'gyp'))
from util import build_utils

# six is a dependency of javalang
sys.path.insert(
    1,
    os.path.join(
        os.path.dirname(__file__), os.pardir, os.pardir, os.pardir,
        'third_party', 'six', 'src'))
sys.path.insert(
    1,
    os.path.join(
        os.path.dirname(__file__), os.pardir, os.pardir, os.pardir,
        'third_party', 'javalang', 'src'))
import javalang

_PARAM_TEMPLATE = '{TYPE} {NAME}'
_METHOD_TEMPLATE = ('{MODIFIERS} {RETURN_TYPE} {NAME} ({PARAMS}) '
                    '{{ return {RETURN_VAL}; }}')
_FILE_TEMPLATE = '''\
// Copyright {YEAR} The Chromium Authors. All rights reserved.
// Use of this source code is governed by a BSD-style license that can be
// found in the LICENSE file.
//
// This file is autogenerated by
//     {SCRIPT_NAME}
// Please do not change its content or use it in actual code ({DNS}).

package {PACKAGE};

{IMPORTS}

{MODIFIERS} class {CLASS_NAME} {{
{METHODS}
}}
'''


def _GetScriptName():
  script_components = os.path.abspath(__file__).split(os.path.sep)
  chrome_index = 0
  for idx, value in enumerate(script_components):
    if value == 'chrome':
      chrome_index = idx
      break
  return os.sep.join(script_components[chrome_index:])


def _GetDefaultReturnVal(type_name):
  if type_name in ('byte', 'short', 'int', 'long', 'float', 'double'):
    return '0'
  elif type_name == 'boolean':
    return 'false'
  elif type_name == 'void':
    return ''
  else:
    return 'null'


def _ParseImports(imports):
  """Returns dict mapping from type name to import path."""
  import_dict = {}
  for import_ in imports:
    if import_.static:
      continue
    assert not import_.wildcard
    name = import_.path.split('.')[-1]
    import_dict[name] = import_.path
  return import_dict


def _ParsePublicMethodsSignatureTypes(clazz):
  """Returns set of type names used in the signatures of all public methods of
  the given class.
  """
  types = set()
  for method in clazz.methods:
    if 'public' in method.modifiers:
      for p in method.parameters:
        types.update(_GetNames(p.type))
      types.update(_GetNames(method.return_type))
  return types


def _GetNames(type_node):
  # Void methods have None as its return_type, taking care of this here makes
  # calling code more readable.
  if type_node is None:
    return []
  if isinstance(type_node, javalang.tree.ReferenceType):
    # TODO: Support sub_type if someone wants to use it.
    names = [type_node.name]
    if type_node.arguments:
      for arg in type_node.arguments:
        names.extend(_GetNames(arg))
    return names
  if isinstance(type_node, javalang.tree.TypeArgument):
    # TODO: Support pattern_type if someone wants to use it.
    return _GetNames(type_node.type)
  if isinstance(type_node, javalang.tree.BasicType):
    # TODO: Support dimensions if someone wants to use it.
    return [type_node.name]
  assert False, 'Unknown type_node={}'.format(type_node)


def _FormatType(type_node):
  if type_node is None:
    return 'void'
  if isinstance(type_node, javalang.tree.ReferenceType):
    # TODO: Support sub_type if someone wants to use it.
    if not type_node.arguments:
      return type_node.name
    formatted_args = (_FormatType(arg) for arg in type_node.arguments)
    return '{name}<{arguments}>'.format(
      name=type_node.name, arguments=','.join(formatted_args))
  if isinstance(type_node, javalang.tree.TypeArgument):
    # TODO: Support pattern_type if someone wants to use it.
    return _FormatType(type_node.type)
  if isinstance(type_node, javalang.tree.BasicType):
    # TODO: Support dimensions if someone wants to use it.
    return type_node.name
  assert False, 'Type node {node} cannot be formatted.'.format(node=type_node)


def _FormatMethod(method):
  params = []
  for param in method.parameters:
    param_dict = {
        'TYPE': _FormatType(param.type),
        'NAME': param.name,
    }
    params.append(_PARAM_TEMPLATE.format(**param_dict))
  return_type = _FormatType(method.return_type)
  method_dict = {
      'MODIFIERS': ' '.join(method.modifiers),
      'RETURN_TYPE': return_type,
      'NAME': method.name,
      'PARAMS': ', '.join(params),
      'RETURN_VAL': _GetDefaultReturnVal(return_type),
  }
  return (_METHOD_TEMPLATE.format(**method_dict))


def _FormatPublicMethods(clazz):
  methods = []
  for method in clazz.methods:
    if 'public' in method.modifiers:
      methods.append(_FormatMethod(method))
  return methods


def _FilterAndFormatImports(import_dict, signature_types):
  """Returns formatted imports required by the passed signature types."""
  formatted_imports = [
      'import %s;' % import_dict[t] for t in signature_types if t in import_dict
  ]
  return sorted(formatted_imports)


def main(args):
  parser = argparse.ArgumentParser()
  parser.add_argument('--input', required=True, help='Input java file path.')
  parser.add_argument('--output', required=True, help='Output java file path.')
  options = parser.parse_args(args)

  with open(options.input, 'r') as f:
    content = f.read()

  java_ast = javalang.parse.parse(content)
  assert len(java_ast.types) == 1, 'Can only process Java files with one class'
  clazz = java_ast.types[0]
  import_dict = _ParseImports(java_ast.imports)
  signature_types = _ParsePublicMethodsSignatureTypes(clazz)
  formatted_public_methods = _FormatPublicMethods(clazz)
  formatted_imports = _FilterAndFormatImports(import_dict, signature_types)

  file_dict = {
      # This is necessary for this file to not trigger presubmit errors.
      'DNS': ' '.join(['DO', 'NOT', 'SUBMIT']),
      'YEAR': str(datetime.date.today().year),
      'SCRIPT_NAME': _GetScriptName(),
      'PACKAGE': java_ast.package.name,
      'IMPORTS': '\n'.join(formatted_imports),
      'MODIFIERS': ' '.join(clazz.modifiers),
      'CLASS_NAME': clazz.name,
      'METHODS': '\n'.join(['    ' + m for m in formatted_public_methods])
  }
  with build_utils.AtomicOutput(options.output) as f:
    f.write(_FILE_TEMPLATE.format(**file_dict))


if __name__ == '__main__':
  main(sys.argv[1:])
