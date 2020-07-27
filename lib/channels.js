import { channelsService } from './services'
import {
  logAndExit,
  execute,
  createStandardDelete
} from './helpers'
const nconf = require('nconf')

export const deleteChannels = createStandardDelete({
  itemName: 'channels',
  service: channelsService
})

export const importChannels = () =>
  Promise.all(
    require(process.cwd() + '/data/channels.json').map(
      (element) =>
        execute({
          uri: channelsService.build(),
          method: 'POST',
          body: element
        })
    )
  ).then(
    () =>
      // eslint-disable-next-line no-console
      console.log('\x1b[32m%s\x1b[0m', 'Channels imported'),
    (err) => logAndExit(err, 'Failed to import channels')
  )

if (nconf.get('clean')) {
  deleteChannels()
} else if (nconf.get('import')) {
  // eslint-disable-next-line no-console
  console.log('\x1b[32m%s\x1b[0m', 'Importing channels...')
  importChannels()
}
